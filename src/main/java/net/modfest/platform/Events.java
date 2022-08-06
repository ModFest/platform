package net.modfest.platform;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import discord4j.core.spec.MessageCreateSpec;
import net.modfest.platform.data.DataManager;
import net.modfest.platform.data.StorageManager;
import net.modfest.platform.discord.modal.Modals;
import net.modfest.platform.pojo.EventData;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Events {
    private static final Logger LOGGER = Loggers.getLogger(ModFestPlatform.class);
    public static final Function<MemberJoinEvent, Publisher<Void>> ON_MEMEBER_JOIN = event -> {
        if (event.getGuildId().toString().equals(DataManager.getGuildId())) {
            var member = event.getMember();
            var data = DataManager.getUserData(member.getId());
            var userRole = DataManager.getUserRole();
            if (data != null) {
                var publisher = member.addRole(userRole);
                for (EventData eventData : DataManager.getEventList()) {
                    var participantData = eventData.participants.get(member.getId().asString());
                    if (participantData != null) {
                        publisher = publisher.and(member.addRole(Snowflake.of(eventData.participantRoleId)));
                        for (String submission : participantData.submissions) {
                            var submissionData = DataManager.getSubmissions().get(submission);
                            if (submissionData.awarded) {
                                publisher = publisher.and(member.addRole(Snowflake.of(eventData.awardRoleId)));
                                break;
                            }
                        }
                    }
                }
                return publisher;
            }
        }
        return Mono.empty();
    };
    public static final Function<ReadyEvent, Publisher<Void>> ON_READY = event -> {
        var client = event.getClient();
        var registerGuildCommands = GuildCommandRegistrar.create(client.getRestClient(), Commands.getGuildCommands())
                .registerCommands()
                .doOnError(e -> LOGGER.warn("Unable to create guild command", e))
                .onErrorResume(e -> Mono.empty());

        var registerGlobalCommands = GlobalCommandRegistrar.create(client.getRestClient(), Commands.getGlobalCommands())
                .registerCommands()
                .doOnError(e -> LOGGER.warn("Unable to create global command", e))
                .onErrorResume(e -> Mono.empty());

        return client.updatePresence(ClientPresence.of(Status.ONLINE, ClientActivity.playing(ModFestPlatform.activeEvent.name)))
                .and(registerGuildCommands)
                .and(registerGlobalCommands);
    };
    public static Function<ChatInputInteractionEvent, Publisher<Void>> ON_CHAT_INPUT_INTERACTION = event -> {
        InteractionApplicationCommandCallbackReplyMono NOT_REGISTERED_MESSAGE = event.reply("You have not yet registered for ModFest. Register with the `/register` command.")
                .withEphemeral(true);
        if (event.getCommandName().equals("unregister")) {
            return DataManager.unregister(event.getInteraction().getMember().get(), DataManager.getActiveEvent().id)
                    .and(event.reply("You are no longer registered for " + DataManager.getActiveEvent().name)
                            .withEphemeral(true));
        } else if (event.getCommandName().equals("admin")) {
            if (event.getOption("reload").isPresent()) {
                StorageManager.loadAll();
                if (ModFestPlatform.activeEvent != null) {
                    return event.reply("Reloaded!")
                            .withEphemeral(true)
                            .and(event.getClient()
                                    .updatePresence(ClientPresence.of(Status.ONLINE, ClientActivity.playing(ModFestPlatform.activeEvent.name))));
                }
                return event.reply("Reloaded!").withEphemeral(true);
            } else if (event.getOption("openregistration").isPresent()) {
                if (ModFestPlatform.activeEvent != null) {

                    Button registerButton = Button.primary("modfest-registration-button", "Register");

                    return event.getInteraction()
                            .getChannel()
                            .ofType(GuildMessageChannel.class)
                            .flatMap(channel -> channel.createMessage(MessageCreateSpec.builder()
                                    .addEmbed(EmbedCreateSpec.builder()
                                            .title("Register for " + ModFestPlatform.activeEvent.name + "!")
                                            .description("Registrations are now open! Click on the button below (or use the `/register` command) to register yourself for " + ModFestPlatform.activeEvent.name + ". If this is your first time registering, you will be prompted with a form to collect your preferred name, pronouns, and your Modrinth username.\n\nIf you change your mind about participating, you can use the `/unregister` command to remove yourself from " + ModFestPlatform.activeEvent.name + ".")
                                            .build())
                                    .addComponent(ActionRow.of(registerButton))
                                    .build()))
                            .then();
                }
            } else if (event.getOption("fixroles").isPresent()) {
                var fixRoles = event.getClient()
                        .getGuildMembers(Snowflake.of(DataManager.getGuildId()))
                        .flatMap(member -> {
                            Mono<Void> publisher;
                            var userData = DataManager.getUserData(member.getId());
                            var userRole = DataManager.getUserRole();
                            if (userData != null) {
                                publisher = member.addRole(userRole);
                            } else {
                                publisher = member.removeRole(userRole);
                            }
                            for (EventData eventData : DataManager.getEventList()) {
                                var participantRole = Snowflake.of(eventData.participantRoleId);
                                var awardRole = Snowflake.of(eventData.awardRoleId);

                                var participantData = eventData.participants.get(member.getId().asString());
                                if (participantData != null) {
                                    publisher = publisher.and(member.addRole(participantRole));
                                    for (String submission : participantData.submissions) {
                                        var submissionData = DataManager.getSubmissions().get(submission);
                                        if (submissionData.awarded) {
                                            publisher = publisher.and(member.addRole(awardRole));
                                            break;
                                        }
                                    }
                                } else {
                                    publisher = publisher.and(member.removeRole(participantRole))
                                            .and(member.removeRole(awardRole));
                                }
                            }
                            return publisher;
                        });
                return fixRoles.then(event.reply("Fixed any incorrect roles").withEphemeral(true));
            }
        } else if (event.getCommandName().equals("user")) {
            var userId = event.getInteraction().getMember().get().getId();
            if (DataManager.getUserData(userId) == null) {
                return NOT_REGISTERED_MESSAGE;
            }
            if (event.getOption("syncdata").isPresent()) {
                var error = DataManager.updateUserData(userId);
                if (error != null) {
                    return event.reply("Error updating data: " + error).withEphemeral(true);
                }
                return event.reply("Updated your user data from Modrinth").withEphemeral(true);
            } else {
                var setOption = event.getOption("set");
                if (setOption.isPresent()) {
                    var set = setOption.get();
                    List<String> updated = new ArrayList<>();
                    boolean plural = false;
                    var name = set.getOption("name");
                    if (name.isPresent()) {
                        DataManager.updateUserDisplayName(userId, name.get().getValue().get().asString());
                        updated.add("display name");
                    }
                    var pronouns = set.getOption("pronouns");
                    if (pronouns.isPresent()) {
                        DataManager.updateUserPronouns(userId, pronouns.get().getValue().get().asString());
                        updated.add("pronouns");
                        plural = true;
                    }
                    if (updated.isEmpty()) {
                        return event.reply("No changes were made to your user data").withEphemeral(true);
                    }
                    StringBuilder string = new StringBuilder();
                    var totalCount = updated.size();
                    for (int i = 0; i < totalCount; i++) {
                        String item = updated.get(i);
                        string.append(item);
                        if (i == totalCount - 2) {
                            if (i == 0) {
                                string.append(" and ");
                            } else {
                                string.append(", and ");
                            }
                        } else if (i != totalCount - 1) {
                            string.append(", ");
                        }
                    }
                    if (totalCount > 1) {
                        plural = true;
                    }
                    return event.reply("Your " + string + (plural ? " have" : " has") + " been updated")
                            .withEphemeral(true);
                }
            }
        }
        var modal = Modals.MODAL_COMMANDS.get(event.getCommandName());
        if (modal != null) {
            return modal.onCommand(event);
        }
        return Mono.empty();
    };
    public static Function<ModalSubmitInteractionEvent, Publisher<Void>> ON_MODAL_SUBMIT = event -> {
        var modal = Modals.MODAL_IDS.get(event.getCustomId());
        if (modal != null) {
            return modal.onSubmit(event);
        }
        return Mono.empty();
    };
    public static Function<ButtonInteractionEvent, Publisher<Void>> ON_BUTTON_INTERACTION = event -> {
        if (event.getCustomId().equals("modfest-registration-button")) {
            return Modals.MODAL_COMMANDS.get("register").onCommand(event);
        }
        return Mono.empty();
    };
}
