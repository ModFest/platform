package net.modfest.platform.discord.modal;

import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.TextInput;
import net.modfest.platform.ModFestPlatform;
import net.modfest.platform.data.DataManager;
import net.modfest.platform.data.StorageManager;
import net.modfest.platform.log.ModFestLog;
import net.modfest.platform.modrinth.Modrinth;
import net.modfest.platform.pojo.UserData;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public class RegisterModal extends Modal {
    public final String displayNameInput = textInput("display-name",
            "What name would you like on your profile?",
            true,
            3,
            24);
    public final String modrinthUsernameInput = textInput("modrinth-slug", "What's your Modrinth slug?", true, 1, 24);
    public final String pronounsInput = textInput("pronouns", "What are your preferred pronouns?", false, 1, 24);

    public RegisterModal() {
        super("register", "Register for " + ModFestPlatform.activeEvent.name());
    }

    private static Mono<Void> register(DeferrableInteractionEvent event) {
        Publisher<?> addRole = DataManager.register(event.getInteraction().getMember().get(),
                ModFestPlatform.activeEvent.id());
        return event.reply("You're now registered for " + ModFestPlatform.activeEvent.name())
                .withEphemeral(true)
                .and(addRole);
    }

    @Override
    public Publisher<Void> checkConditions(DeferrableInteractionEvent event) {
        var conditions = super.checkConditions(event);
        if (conditions != null) {
            return conditions;
        }
        if (ModFestPlatform.activeEvent == null || !ModFestPlatform.activeEvent.phase()
                .canRegister()) {
            return event.reply(
                            "ModFest registrations are not currently open. Make sure @everyone mentions are enabled to be notified when the next ModFest event goes live.")
                    .withEphemeral(true);
        }
        var member = event.getInteraction().getMember().get();
        var userId = member.getId();
        if (DataManager.getUser(userId) != null) {
            if (DataManager.isRegistered(userId, ModFestPlatform.activeEvent.id())) {
                return event.reply("You're already registered for " + ModFestPlatform.activeEvent.name())
                        .withEphemeral(true);
            } else {
                ModFestLog.debug("[RegisterModal] Registering member (" + member.getUsername() + "/" + member.getId()
                        .asString() + ") to event '" + DataManager.getActiveEvent().id() + "'");
                return register(event);
            }
        }
        return null;
    }

    @Override
    public Publisher<Void> onSubmit(ModalSubmitInteractionEvent event) {
        if (ModFestPlatform.activeEvent == null && requiresActiveEvent()) {
            return event.reply("An error has occurred: There is no active ModFest event.").withEphemeral(true);
        } else {
            String displayName = "";
            String modrinthUsername = "";
            String pronouns = "";

            for (TextInput component : event.getComponents(TextInput.class)) {
                String customId = component.getCustomId();
                if (displayNameInput.equals(customId)) {
                    displayName = component.getValue().orElse("");
                } else if (pronounsInput.equals(customId)) {
                    pronouns = component.getValue().orElse("");
                } else if (modrinthUsernameInput.equals(customId)) {
                    modrinthUsername = component.getValue().orElse("");
                }
            }

            if (displayName.isBlank()) {
                return event.reply("An error has occurred: your display name cannot be blank.").withEphemeral(true);
            }

            var member = event.getInteraction()
                    .getMember()
                    .orElseThrow(() -> new NullPointerException("Member is null for RegisterModal submission."));

            var snowflake = member.getId();
            if (DataManager.getUser(snowflake) != null) {
                return event.reply("An error has occurred: you are already registered.").withEphemeral(true);
            }

            try {
                var modrinthUser = Modrinth.getUser(modrinthUsername);
                DataManager.addUserData(snowflake,
                        new UserData(StorageManager.uniqueify(modrinthUsername,
                                str -> DataManager.getUsers().containsKey(str)),
                                StorageManager.uniqueify(modrinthUsername,
                                        str -> DataManager.getUsers()
                                                .values()
                                                .stream()
                                                .anyMatch(userData -> userData.slug().equals(str))),
                                displayName,
                                pronouns,
                                modrinthUser.id,
                                snowflake.asString(),
                                modrinthUser.bio,
                                modrinthUser.avatarUrl,
                                null,
                                null));

                return register(event);
            } catch (Throwable e) {
                e.printStackTrace();
                return event.reply("An error has occurred finding Modrinth user '" + modrinthUsername + "': " + e.getMessage())
                        .withEphemeral(true);
            }
        }
    }

    @Override
    public boolean requiresActiveEvent() {
        return true;
    }
}
