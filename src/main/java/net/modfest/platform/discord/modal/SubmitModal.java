package net.modfest.platform.discord.modal;

import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.TextInput;
import fr.minemobs.modrinthjavalib.Modrinth;
import fr.minemobs.modrinthjavalib.project.version.FilesItem;
import net.modfest.platform.ModFestPlatform;
import net.modfest.platform.data.DataManager;
import net.modfest.platform.pojo.SubmissionData;
import org.reactivestreams.Publisher;

import java.util.regex.Pattern;

public class SubmitModal extends Modal {
    public final String modrinthProjectUrlInput = textInput("modrinth-project-url", "Enter your project's Modrinth URL", true, 10, 128);

    public SubmitModal() {
        super("submit", "Submit a mod");
    }

    @Override
    public Publisher<Void> checkConditions(DeferrableInteractionEvent event) {
        var conditions = super.checkConditions(event);
        if (conditions != null) {
            return conditions;
        }
        var member = event.getInteraction().getMember().get();
        var userId = member.getId();
        if (DataManager.getUserData(userId) == null || !DataManager.isRegistered(userId, ModFestPlatform.activeEvent.id)) {
            return event.reply("Error: You are not registered for " + ModFestPlatform.activeEvent.name)
                    .withEphemeral(true);
        }
        return null;
    }

    @Override
    public Publisher<Void> onSubmit(ModalSubmitInteractionEvent event) {
        if (ModFestPlatform.activeEvent == null && requiresActiveEvent()) {
            return event.reply("An error has occurred: There is no active ModFest event.").withEphemeral(true);
        } else {
            String modrinthProjectUrl = "";

            for (TextInput component : event.getComponents(TextInput.class)) {
                String customId = component.getCustomId();
                if (modrinthProjectUrlInput.equals(customId)) {
                    modrinthProjectUrl = component.getValue().orElse("");
                }
            }
            var regex = Pattern.compile(".*modrinth\\.com/mod/(\\w+)/?.*");
            var matcher = regex.matcher(modrinthProjectUrl);

            if (!matcher.find()) {
                return event.reply("An error has occurred: Could not parse Modrinth project URL.").withEphemeral(true);
            }

            var member = event.getInteraction()
                    .getMember()
                    .orElseThrow(() -> new NullPointerException("Member is null for SubmitModal submission."));

            var snowflake = member.getId();
            if (DataManager.getUserData(snowflake) == null) {
                return event.reply("An error has occurred: you do not have a ModFest account.").withEphemeral(true);
            }

            try {
                var project = Modrinth.getProject(matcher.group(1));
                var primaryGallery = project.gallery.stream()
                        .filter(item -> item.featured)
                        .findFirst();
                var galleryUrl = "";
                if (primaryGallery.isPresent()) {
                    galleryUrl = primaryGallery.get().url;
                }
                var version = project.getVersions()[0];
                DataManager.addSubmission(snowflake, new SubmissionData(
                        project.id,
                        project.slug,
                        project.title,
                        project.description,
                        project.body,
                        snowflake.asString(),
                        project.iconUrl,
                        galleryUrl,
                        "https://modrinth.com/mod/" + project.slug,
                        version.id,
                        version.getFiles()
                                .stream()
                                .filter(FilesItem::isPrimary)
                                .findFirst()
                                .get().url,
                        project.sourceUrl
                ), DataManager.getActiveEvent().id);

                return event.reply("Mod '" + project.title + "' submitted successfully for " + ModFestPlatform.activeEvent.name)
                        .withEphemeral(true);
            } catch (NullPointerException e) {
                e.printStackTrace();
                return event.reply("Your Modrinth project could not be found. This either means your URL was invalid or your project has not yet been approved by Modrinth moderators. Re-submit to ModFest after it has been approved.").withEphemeral(true);
            } catch (Throwable e) {
                e.printStackTrace();
                return event.reply("An error has occurred finding Modrinth project from URL '" + modrinthProjectUrl + "': " + e.getMessage())
                        .withEphemeral(true);
            }
        }
    }

    @Override
    public boolean requiresActiveEvent() {
        return true;
    }
}
