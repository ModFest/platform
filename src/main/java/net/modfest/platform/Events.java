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
import net.modfest.platform.log.ModFestLog;
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
            ModFestLog.debug("[Events/ON_MEMEBER_JOIN] Member joined (" + member.getUsername() + "/" + member.getId()
                    .asString() + ")");
            var data = DataManager.getUserData(member.getId());
            var userRole = DataManager.getUserRole();
            if (data != null) {
                ModFestLog.debug("[Events/ON_MEMEBER_JOIN] Member has data (" + member.getUsername() + "/" + member.getId()
                        .asString() + ")");
                var publisher = member.addRole(userRole);
                for (EventData eventData : DataManager.getEventList()) {
                    var participantData = eventData.participants.get(member.getId().asString());
                    if (participantData != null) {
                        ModFestLog.debug("[Events/ON_MEMEBER_JOIN] Member has participant data for event '" + eventData.id + "', granting role (" + member.getUsername() + "/" + member.getId()
                                .asString() + ")");
                        publisher = publisher.and(member.addRole(Snowflake.of(eventData.participantRoleId)));
                        for (String submission : participantData.submissions) {
                            var submissionData = DataManager.getSubmissions().get(submission);
                            if (submissionData.awarded) {
                                ModFestLog.debug("[Events/ON_MEMEBER_JOIN] Member has award for event '" + eventData.id + "', granting role (" + member.getUsername() + "/" + member.getId()
                                        .asString() + ")");
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
        ModFestLog.debug("[Events/ON_READY] Registering guild commands");
        var registerGuildCommands = GuildCommandRegistrar.create(client.getRestClient(), Commands.getGuildCommands())
                .registerCommands()
                .doOnError(e -> LOGGER.warn("Unable to create guild command", e))
                .onErrorResume(e -> Mono.empty());

        ModFestLog.debug("[Events/ON_READY] Registering global commands");
        var registerGlobalCommands = GlobalCommandRegistrar.create(client.getRestClient(), Commands.getGlobalCommands())
                .registerCommands()
                .doOnError(e -> LOGGER.warn("Unable to create global command", e))
                .onErrorResume(e -> Mono.empty());

        ModFestLog.debug("[Events/ON_READY] Updating presence");
        return client.updatePresence(ClientPresence.of(Status.ONLINE, ClientActivity.playing(ModFestPlatform.activeEvent.name)))
                .and(registerGuildCommands)
                .and(registerGlobalCommands);
    };
    public static Function<ChatInputInteractionEvent, Publisher<Void>> ON_CHAT_INPUT_INTERACTION = event -> {
        InteractionApplicationCommandCallbackReplyMono NOT_REGISTERED_MESSAGE = event.reply("You have not yet registered for ModFest. Register with the `/register` command.")
                .withEphemeral(true);
        var member = event.getInteraction().getMember().get();
        if (event.getCommandName().equals("unregister")) {
            ModFestLog.debug("[Events/ON_CHAT_INPUT_INTERACTION/unregister] Unregistering member (" + member.getUsername() + "/" + member.getId()
                    .asString() + ") from event '" + DataManager.getActiveEvent().id + "'");
            return DataManager.unregister(member, DataManager.getActiveEvent().id)
                    .and(event.reply("You are no longer registered for " + DataManager.getActiveEvent().name)
                            .withEphemeral(true));
        } else if (event.getCommandName().equals("admin")) {
            ModFestLog.debug("[Events/ON_CHAT_INPUT_INTERACTION/admin] Running admin command (" + member.getUsername() + "/" + member.getId()
                    .asString() + ")");
            if (event.getOption("reload").isPresent()) {
                ModFestLog.debug("[Events/ON_CHAT_INPUT_INTERACTION/admin/reload] Running reload command (" + member.getUsername() + "/" + member.getId()
                        .asString() + ")");
                StorageManager.loadAll();
                if (ModFestPlatform.activeEvent != null) {
                    ModFestLog.debug("[Events/ON_CHAT_INPUT_INTERACTION/admin/reload] Updating presence (" + member.getUsername() + "/" + member.getId()
                            .asString() + ")");
                    return event.reply("Reloaded!")
                            .withEphemeral(true)
                            .and(event.getClient()
                                    .updatePresence(ClientPresence.of(Status.ONLINE, ClientActivity.playing(ModFestPlatform.activeEvent.name))));
                }
                return event.reply("Reloaded!").withEphemeral(true);
            } else if (event.getOption("openregistration").isPresent()) {
                ModFestLog.debug("[Events/ON_CHAT_INPUT_INTERACTION/admin/openregistration] Opening registration (" + member.getUsername() + "/" + member.getId()
                        .asString() + ")");
                if (ModFestPlatform.activeEvent != null) {

                    Button registerButton = Button.primary("modfest-registration-button", "Register");

                    var openInChannel = event.getInteraction().getChannel();
                    return openInChannel.ofType(GuildMessageChannel.class)
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
                ModFestLog.debug("[Events/ON_CHAT_INPUT_INTERACTION/admin/fixroles] Initiating fixroles (" + member.getUsername() + "/" + member.getId()
                        .asString() + ")");
                var fixRoles = event.getClient()
                        .getGuildMembers(Snowflake.of(DataManager.getGuildId()))
                        .flatMap(guildMember -> {
                            Mono<Void> publisher;
                            var userData = DataManager.getUserData(guildMember.getId());
                            var userRole = DataManager.getUserRole();
                            if (userData != null) {
                                ModFestLog.debug("[Events/ON_CHAT_INPUT_INTERACTION/admin/fixroles] Assigning user role to " + guildMember.getUsername() + "/" + guildMember.getId()
                                        .asString());
                                publisher = guildMember.addRole(userRole);
                            } else {
                                publisher = guildMember.removeRole(userRole);
                            }
                            for (EventData eventData : DataManager.getEventList()) {
                                var participantRole = Snowflake.of(eventData.participantRoleId);
                                var awardRole = Snowflake.of(eventData.awardRoleId);

                                var participantData = eventData.participants.get(guildMember.getId().asString());
                                if (participantData != null) {
                                    ModFestLog.debug("[Events/ON_CHAT_INPUT_INTERACTION/admin/fixroles] Assigning '" + eventData.id + "' participant role to " + guildMember.getUsername() + "/" + guildMember.getId()
                                            .asString());
                                    publisher = publisher.and(guildMember.addRole(participantRole));
                                    for (String submission : participantData.submissions) {
                                        var submissionData = DataManager.getSubmissions().get(submission);
                                        if (submissionData.awarded) {
                                            ModFestLog.debug("[Events/ON_CHAT_INPUT_INTERACTION/admin/fixroles] Assigning '" + eventData.id + "' award role to " + guildMember.getUsername() + "/" + guildMember.getId()
                                                    .asString());
                                            publisher = publisher.and(guildMember.addRole(awardRole));
                                            break;
                                        }
                                    }
                                } else {
                                    publisher = publisher.and(guildMember.removeRole(participantRole))
                                            .and(guildMember.removeRole(awardRole));
                                }
                            }
                            return publisher;
                        });
                ModFestLog.debug("[Events/ON_CHAT_INPUT_INTERACTION/admin/fixroles] Fixroles complete (" + member.getUsername() + "/" + member.getId()
                        .asString() + ")");
                return fixRoles.then(event.reply("Fixed any incorrect roles").withEphemeral(true));
            }
        } else if (event.getCommandName().equals("user")) {
            var userId = member.getId();
            ModFestLog.debug("[Events/ON_CHAT_INPUT_INTERACTION/user] Running user command (" + member.getUsername() + "/" + userId.asString() + ")");
            if (DataManager.getUserData(userId) == null) {
                ModFestLog.error("[Events/ON_CHAT_INPUT_INTERACTION/user] User does not have data on file, should not have been able to run command (" + member.getUsername() + "/" + userId.asString() + ")");
                return NOT_REGISTERED_MESSAGE;
            }
            if (event.getOption("syncdata").isPresent()) {
                ModFestLog.debug("[Events/ON_CHAT_INPUT_INTERACTION/user/syncdata] Syncing user data (" + member.getUsername() + "/" + userId.asString() + ")");
                var error = DataManager.updateUserData(userId);
                if (error != null) {
                    ModFestLog.error("[Events/ON_CHAT_INPUT_INTERACTION/user/syncdata] Error syncing user data (" + member.getUsername() + "/" + userId.asString() + "): " + error);
                    return event.reply("Error updating data: " + error).withEphemeral(true);
                }
                ModFestLog.debug("[Events/ON_CHAT_INPUT_INTERACTION/user/syncdata] Synced user data (" + member.getUsername() + "/" + userId.asString() + ")");
                return event.reply("Updated your user data from Modrinth").withEphemeral(true);
            } else {
                var setOption = event.getOption("set");
                if (setOption.isPresent()) {
                    var set = setOption.get();
                    List<String> updated = new ArrayList<>();
                    boolean plural = false;
                    var name = set.getOption("name");
                    if (name.isPresent()) {
                        var newName = name.get().getValue().get().asString();
                        ModFestLog.debug("[Events/ON_CHAT_INPUT_INTERACTION/user/set/name] Updating display name to '" + name + "' (" + member.getUsername() + "/" + userId.asString() + ")");
                        DataManager.updateUserDisplayName(userId, newName);
                        updated.add("display name");
                    }
                    var pronouns = set.getOption("pronouns");
                    if (pronouns.isPresent()) {
                        var newPronouns = pronouns.get().getValue().get().asString();
                        DataManager.updateUserPronouns(userId, newPronouns);
                        ModFestLog.debug("[Events/ON_CHAT_INPUT_INTERACTION/user/set/pronouns] Updating pronouns to '" + newPronouns + "' (" + member.getUsername() + "/" + userId.asString() + ")");
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
