package net.modfest.platform.discord.modal;

import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.TextInput;
import net.modfest.platform.ModFestPlatform;
import net.modfest.platform.data.DataManager;
import net.modfest.platform.data.StorageManager;
import net.modfest.platform.modrinth.Modrinth;
import net.modfest.platform.modrinth.project.version.FilesItem;
import net.modfest.platform.pojo.SubmissionData;
import net.modfest.platform.pojo.UserData;
import org.reactivestreams.Publisher;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class SubmitModal extends Modal {
    public final String modrinthProjectUrlInput = textInput("modrinth-project-url",
            "Enter your project's Modrinth URL",
            true,
            10,
            128,
            "https://modrinth.com/mod/your-mod");

    public SubmitModal() {
        super("submit", "Submit a mod");
    }

    @Override
    public Publisher<Void> checkConditions(DeferrableInteractionEvent event) {
        var conditions = super.checkConditions(event);
        if (conditions != null) {
            return conditions;
        }
        if (ModFestPlatform.activeEvent == null || !ModFestPlatform.activeEvent.phase()
                .canSubmit()) {
            return event.reply(
                            "ModFest submissions are not currently open. Make sure @everyone mentions are enabled to be notified when the next ModFest event goes live.")
                    .withEphemeral(true);
        }
        var member = event.getInteraction().getMember().get();
        var userId = member.getId();
        if (DataManager.getUser(userId) == null || !DataManager.isRegistered(userId,
                ModFestPlatform.activeEvent.id())) {
            return event.reply("Error: You are not registered for " + ModFestPlatform.activeEvent.name() + ", run /register first!")
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

            String decodedUrl;
            decodedUrl = URLDecoder.decode(modrinthProjectUrl, StandardCharsets.UTF_8);
            var regex = Pattern.compile(".*modrinth\\.com/mod/([\\w!@$()`.+,\"\\-']{3,64})/?.*");
            var matcher = regex.matcher(decodedUrl);

            if (!matcher.find()) {
                return event.reply("An error has occurred: Could not parse Modrinth project URL.").withEphemeral(true);
            }

            var member = event.getInteraction()
                    .getMember()
                    .orElseThrow(() -> new NullPointerException("Member is null for SubmitModal submission."));

            var snowflake = member.getId();
            if (DataManager.getUser(snowflake) == null) {
                return event.reply("An error has occurred: you do not have a ModFest account.").withEphemeral(true);
            }

            try {
                var project = Modrinth.getProject(matcher.group(1));
                var version = project.getVersions()[0];
                List<FilesItem> files = version.getFiles();
                UserData user = DataManager.getUser(snowflake);
                DataManager.addSubmission(user.id(),
                        new SubmissionData(StorageManager.uniqueify(project.slug,
                                str -> DataManager.getSubmissionsFlat().containsKey(str)),
                                project.title,
                                project.description,
                                Set.of(user.id()),
                                new SubmissionData.Platform.Modrinth(project.id, version.id),
                                project.getImages(),
                                files.stream().filter(FilesItem::isPrimary).findFirst().orElseGet(files::getFirst).url,
                                project.sourceUrl,
                                null, ModFestPlatform.activeEvent.id()),
                        DataManager.getActiveEvent().id());

                return event.reply("Mod '" + project.title + "' submitted successfully for " + ModFestPlatform.activeEvent.name())
                        .withEphemeral(true);
            } catch (NullPointerException e) {
                e.printStackTrace();
                return event.reply(
                                "Your Modrinth project could not be found. This either means your URL was invalid or your project has not yet been approved by Modrinth moderators. Re-submit to ModFest after it has been approved.")
                        .withEphemeral(true);
            } catch (Throwable e) {
                e.printStackTrace();
                return event.reply("An error has occurred finding Modrinth project from URL '" + decodedUrl + "': " + e.getMessage())
                        .withEphemeral(true);
            }
        }
    }

    @Override
    public boolean requiresActiveEvent() {
        return true;
    }
}
