package net.modfest.platform;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.interaction.*;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import net.modfest.platform.data.DataManager;
import net.modfest.platform.data.StorageManager;
import net.modfest.platform.discord.modal.Modals;
import net.modfest.platform.log.ModFestLog;
import net.modfest.platform.modrinth.Modrinth;
import net.modfest.platform.pojo.EventData;
import net.modfest.platform.pojo.SubmissionData;
import net.modfest.platform.pojo.UserData;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

public class Events {
    public static final Function<MemberJoinEvent, Publisher<Void>> ON_MEMEBER_JOIN = event -> {
        if (event.getGuildId().toString().equals(DataManager.getGuildId())) {
            var member = event.getMember();
            ModFestLog.debug("[Events/ON_MEMEBER_JOIN] Member joined (" + member.getUsername() + "/" + member.getId()
                    .asString() + ")");
            return fixMemberRoles(member);
        }
        return Mono.empty();
    };

    public static Mono<Void> fixMemberRoles(Member member) {
        var user = DataManager.getUser(member.getId());
        var userRole = DataManager.getUserRole();
        if (user != null) {
            ModFestLog.debug("[Events/fixMemberRoles] Member has data (" + member.getUsername() + "/" + member.getId()
                    .asString() + ")");
            var publisher = member.addRole(userRole);
            for (SubmissionData submission : user.getSubmissions()) {
                EventData event = submission.getEvent();
                ModFestLog.debug("[Events/fixMemberRoles] Member has participant data for event '" + event.id() + "', granting role (" + member.getUsername() + "/" + member.getId()
                        .asString() + ")");
                EventData.DiscordRoles roles = event.discordRoles();
                publisher = publisher.and(member.addRole(Snowflake.of(roles.participant())));
                var awards = submission.awards();
                if (awards != null) {
                    Set<String> themeAwards = awards.theme();
                    if (themeAwards != null && !themeAwards.isEmpty()) {
                        publisher = publisher.and(member.addRole(Snowflake.of(roles.award())));
                    }
                    ModFestLog.debug("[Events/fixMemberRoles] Member has award for event '" + event.id() + "', granting role (" + member.getUsername() + "/" + member.getId()
                            .asString() + ")");
                }
            }
            return publisher;
        }
        return Mono.empty();
    }

    public static final Function<ReadyEvent, Publisher<Void>> ON_READY = event -> {
        var client = event.getClient();
        ModFestLog.debug("[Events/ON_READY] Registering guild commands");
        var registerGuildCommands = GuildCommandRegistrar.create(client.getRestClient(), Commands.getGuildCommands())
                .registerCommands()
                .doOnError(e -> ModFestLog.error("Unable to create guild command", e))
                .onErrorResume(e -> Mono.empty());

        ModFestLog.debug("[Events/ON_READY] Registering global commands");
        var registerGlobalCommands = GlobalCommandRegistrar.create(client.getRestClient(), Commands.getGlobalCommands())
                .registerCommands()
                .doOnError(e -> ModFestLog.error("Unable to create global command", e))
                .onErrorResume(e -> Mono.empty());

        ModFestLog.debug("[Events/ON_READY] Updating presence");
        EventData activeEvent = ModFestPlatform.activeEvent;
        return client.updatePresence(ClientPresence.of(Status.ONLINE,
                        ClientActivity.playing(activeEvent != null ? activeEvent.name() : "ModFest")))
                .and(registerGuildCommands)
                .and(registerGlobalCommands);
    };
    public static Function<ChatInputInteractionEvent, Publisher<Void>> ON_CHAT_INPUT_INTERACTION = event -> {
        InteractionApplicationCommandCallbackReplyMono NOT_REGISTERED_MESSAGE = event.reply(
                "You have not yet registered for ModFest. Register with the `/register` command.").withEphemeral(true);
        var member = event.getInteraction().getMember().get();
        ModFestLog.debug("Command received from " + member.getUsername() + "/" + member.getId()
                .asString() + ": " + event.getCommandName());
        if (event.getCommandName().equals("fixmyroles")) {
            ModFestLog.debug("[Events/ON_CHAT_INPUT_INTERACTION/user/fixmyroles] Fixing roles for (" + member.getUsername() + "/" + member.getId()
                    .asString() + ")");
            return fixMemberRoles(member).then(event.reply("Your roles have been fixed.").withEphemeral(true));
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
                                    .updatePresence(ClientPresence.of(Status.ONLINE,
                                            ClientActivity.playing(ModFestPlatform.activeEvent.name()))));
                }
                return event.reply("Reloaded!").withEphemeral(true);
//            } else if (event.getOption("migrate").isPresent()) {
//                ModFestLog.debug("[Events/ON_CHAT_INPUT_INTERACTION/admin/migrate] Running migrate command (" + member.getUsername() + "/" + member.getId()
//                        .asString() + ")");
//                StorageManager.USERS.load();
//
//                // Fix existing users missing ids stored
//                DataManager.getUsers().forEach((id, userData) -> {
//                    if (userData.id == null || userData.id.isEmpty()) {
//                        userData.id = id;
//                    }
//                });
//                StorageManager.USERS.save();
//
//                return event.reply("Migration complete").withEphemeral(true);
            } else if (event.getOption("setactiveevent").isPresent()) {
                ModFestLog.debug(
                        "[Events/ON_CHAT_INPUT_INTERACTION/admin/setactiveevent] Running setactiveevent command (" + member.getUsername() + "/" + member.getId()
                                .asString() + ")");

                return event.reply("Command not yet implemented").withEphemeral(true);
            } else if (event.getOption("setphasemodding").isPresent()) {
                ModFestLog.debug(
                        "[Events/ON_CHAT_INPUT_INTERACTION/admin/setphasemodding] Running setphasemodding command (" + member.getUsername() + "/" + member.getId()
                                .asString() + ")");
                if (ModFestPlatform.activeEvent == null) {
                    return event.reply("There is no active event to set to modding").withEphemeral(true);
                }
                DataManager.setPhase(EventData.Phase.MODDING);
                return event.reply("Set name to MODDING for " + ModFestPlatform.activeEvent.name()).withEphemeral(true);
            } else if (event.getOption("setphaseshowcase").isPresent()) {
                ModFestLog.debug(
                        "[Events/ON_CHAT_INPUT_INTERACTION/admin/setphaseshowcase] Running setphaseshowcase command (" + member.getUsername() + "/" + member.getId()
                                .asString() + ")");
                if (ModFestPlatform.activeEvent == null) {
                    return event.reply("There is no active event to set to showcase").withEphemeral(true);
                }
                DataManager.setPhase(EventData.Phase.SHOWCASE);
                return event.reply("Set name to SHOWCASE for " + ModFestPlatform.activeEvent.name())
                        .withEphemeral(true);
            } else if (event.getOption("setphasecomplete").isPresent()) {
                ModFestLog.debug(
                        "[Events/ON_CHAT_INPUT_INTERACTION/admin/setphasecomplete] Running setphasecomplete command (" + member.getUsername() + "/" + member.getId()
                                .asString() + ")");
                if (ModFestPlatform.activeEvent == null) {
                    return event.reply("There is no active event to set to complete").withEphemeral(true);
                }
                DataManager.setPhase(EventData.Phase.COMPLETE);
                return event.reply("Set name to COMPLETE for " + ModFestPlatform.activeEvent.name())
                        .withEphemeral(true);
            } else if (event.getOption("setphaseplanning").isPresent()) {
                ModFestLog.debug(
                        "[Events/ON_CHAT_INPUT_INTERACTION/admin/setphaseplanning] Running setphaseplanning command (" + member.getUsername() + "/" + member.getId()
                                .asString() + ")");
                if (ModFestPlatform.activeEvent == null) {
                    return event.reply("There is no active event to set to planning").withEphemeral(true);
                }
                DataManager.setPhase(EventData.Phase.PLANNING);
                return event.reply("Set name to PLANNING for " + ModFestPlatform.activeEvent.name())
                        .withEphemeral(true);
            } else if (event.getOption("setphasebuilding").isPresent()) {
                ModFestLog.debug(
                        "[Events/ON_CHAT_INPUT_INTERACTION/admin/setphasebuilding] Running setphasebuilding command (" + member.getUsername() + "/" + member.getId()
                                .asString() + ")");
                if (ModFestPlatform.activeEvent == null) {
                    return event.reply("There is no active event to close submissions for").withEphemeral(true);
                }
                DataManager.setPhase(EventData.Phase.BUILDING);
                Mono<Void> mono = event.reply("Set name to BUILDING for " + ModFestPlatform.activeEvent.name())
                        .withEphemeral(true);
                for (UserData user : DataManager.getUsers().values()) {
                    if (user.registered() != null && !user.registered().isEmpty()) {
                        user.registered().remove(ModFestPlatform.activeEvent.id());
                        String eventId = ModFestPlatform.activeEvent.id();
                        boolean didNotSubmit = user.getSubmissions()
                                .stream()
                                .noneMatch(submission -> submission.getEvent().id().equals(eventId));
                        if (didNotSubmit) {
                            if (!user.discordId().isEmpty()) {
                                mono = mono.and(event.getClient()
                                        .getMemberById(Snowflake.of(DataManager.getGuildId()),
                                                Snowflake.of(user.discordId()))
                                        .doOnError((throwable) -> DataManager.unregister(user, eventId))
                                        .flatMap(participantMember -> DataManager.unregister(participantMember, eventId)));
                            }
                        }
                    }
                }
                return mono;
            } else {
                var fixroles = event.getOption("fixroles");
                if (fixroles.isPresent()) {
                    var selectedUser = fixroles.get().getOption("user");
                    if (selectedUser.isPresent()) {
                        var value = selectedUser.get().getValue();
                        if (value.isPresent()) {
                            return value.get()
                                    .asUser()
                                    .flatMap(fixUser -> fixUser.asMember(Snowflake.of(DataManager.getGuildId()))
                                            .flatMap(fixMember -> fixMemberRoles(fixMember).then(event.reply(fixMember.getDisplayName() + "'s roles have been fixed.")
                                                    .withEphemeral(true))));
                        }
                    }
                } else if (event.getOption("sendregistrationmessage").isPresent()) {
                    Button registerButton = Button.primary("modfest-registration-button", "Register");
                    var currentChannel = event.getInteraction().getChannel();
                    if (ModFestPlatform.activeEvent != null) {
                        return currentChannel.ofType(GuildMessageChannel.class)
                                .flatMap(channel -> channel.createMessage(MessageCreateSpec.builder()
                                        .addEmbed(EmbedCreateSpec.builder()
                                                .title("Register for " + ModFestPlatform.activeEvent.name() + "!")
                                                .description(
                                                        "Registrations are now open! Click on the button below (or use the `/register` command) to register yourself for " + ModFestPlatform.activeEvent.name() + ". If this is your first time registering, you will be prompted with a form to collect your preferred name, pronouns, and your Modrinth slug.\n\nIf you change your mind about participating, you can use the `/event unregister` command to remove yourself from " + ModFestPlatform.activeEvent.name() + ".")
                                                .build())
                                        .addComponent(ActionRow.of(registerButton))
                                        .build()))
                                .then(event.reply("Sent the registration message").withEphemeral(true));
                    }
                } else if (event.getOption("sendsubmissionmessage").isPresent()) {
                    Button submitButton = Button.primary("modfest-submission-button", "Submit a mod");
                    if (ModFestPlatform.activeEvent != null) {
                        var currentChannel = event.getInteraction().getChannel();
                        return currentChannel.ofType(GuildMessageChannel.class)
                                .flatMap(channel -> channel.createMessage(MessageCreateSpec.builder()
                                        .addEmbed(EmbedCreateSpec.builder()
                                                .title("Submit a mod for " + ModFestPlatform.activeEvent.name() + "!")
                                                .description(
                                                        "Submissions are now open! Click on the button below (or use the `/event submit` command) to submit a mod for " + ModFestPlatform.activeEvent.name() + ".\n\nIf you change your mind about submitting, you can use the `/event unsubmit` command to retract your submission. Use `/event updateversion` or `/event setversion` to update the version of your mod that will be included in the ModFest modpack.")
                                                .build())
                                        .addComponent(ActionRow.of(submitButton))
                                        .build()))
                                .then(event.reply("Sent the submission message").withEphemeral(true));
                    }
                }
            }
        } else if (event.getCommandName().equals("user")) {
            var userId = member.getId();
            ModFestLog.debug("[Events/ON_CHAT_INPUT_INTERACTION/user] Running user command (" + member.getUsername() + "/" + userId.asString() + ")");
            if (DataManager.getUser(userId) == null) {
                ModFestLog.error(
                        "[Events/ON_CHAT_INPUT_INTERACTION/user] User does not have data on file, should not have been able to run command (" + member.getUsername() + "/" + userId.asString() + ")");
                return NOT_REGISTERED_MESSAGE;
            } else if (event.getOption("syncdata").isPresent()) {
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
        } else if (event.getCommandName().equals("event")) {
            if (event.getOption("unregister").isPresent()) {
                ModFestLog.debug("[Events/ON_CHAT_INPUT_INTERACTION/unregister] Unregistering member (" + member.getUsername() + "/" + member.getId()
                        .asString() + ") from event '" + (DataManager.hasActiveEvent() ? DataManager.getActiveEvent()
                        .id() : "ModFest") + "'");
                return DataManager.unregister(member, DataManager.getActiveEvent().id())
                        .and(event.reply("You are no longer registered for " + DataManager.getActiveEvent().name())
                                .withEphemeral(true));
            } else if (event.getOption("submit").isPresent()) {
                return Modals.SUBMIT.onCommand(event);
            } else if (event.getOption("unsubmit").isPresent()) {
                var submissionId = event.getOption("unsubmit")
                        .get()
                        .getOption("submission")
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .get()
                        .asString();
                var submission = DataManager.getSubmission(submissionId);
                if (submission == null) {
                    return event.reply("Error: Could not find submission with ID " + submissionId).withEphemeral(true);
                }
                DataManager.removeSubmission(member.getId(), submissionId, DataManager.getActiveEvent().id());
                return event.reply("Successfully unsubmitted " + submission.name()).withEphemeral(true);
            } else if (event.getOption("setversion").isPresent()) {
                var submissionId = event.getOption("setversion")
                        .get()
                        .getOption("submission")
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .get()
                        .asString();
                var submission = DataManager.getSubmission(submissionId);
                if (submission == null) {
                    return event.reply("Error: Could not find submission with ID " + submissionId).withEphemeral(true);
                }
                try {
                    switch (submission.platform()) {
                        case SubmissionData.Platform.Modrinth modrinth -> {
                            var versions = Modrinth.getProject(modrinth.projectId()).getVersions();

                            var options = new ArrayList<SelectMenu.Option>();
                            for (int i = 0; i < Math.min(25, versions.length); i++) {
                                var version = versions[i];
                                var versionNumber = version.getVersionNumber();
                                var prefix = versionNumber.startsWith("v") ? "" : "v";
                                options.add(SelectMenu.Option.of(version.getName(),
                                                submissionId + "/" + version.getId())
                                        .withDescription(prefix + versionNumber + " (ID: " + version.id + ")"));
                            }
                            return event.reply("Select a version of " + submission.name() + ".")
                                    .withComponents(ActionRow.of(SelectMenu.of("modfest-setversion-version-select-menu",
                                            options)))
                                    .withEphemeral(true);
                        }
                        default -> throw new RuntimeException("Submission does not have associated Modrinth project.");
                    }
                } catch (Throwable e) {
                    ModFestLog.error("[Events/event/setversion] Error fetching version data from Modrinth: ", e);
                    return event.reply("Error fetching version data from Modrinth: " + e.getMessage())
                            .withEphemeral(true);
                }
            } else if (event.getOption("updateversion").isPresent()) {
                var submissionId = event.getOption("updateversion")
                        .get()
                        .getOption("submission")
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .get()
                        .asString();
                var submission = DataManager.getSubmission(submissionId);
                if (submission == null) {
                    return event.reply("Error: Could not find submission with ID " + submissionId).withEphemeral(true);
                }
                try {
                    switch (submission.platform()) {
                        case SubmissionData.Platform.Modrinth modrinth -> {
                            var version = Modrinth.getProject(modrinth.projectId()).getVersions()[0];
                            var error = DataManager.setVersion(submissionId, version.id);
                            if (error != null) {
                                return event.reply("An error occurred: " + error).withEphemeral(true);
                            }
                            return event.reply("Set the version of " + submission.name() + " to the latest version: " + version.name + " (v" + version.versionNumber + " / ID: " + version.id + ")")
                                    .withEphemeral(true);
                        }
                        default -> throw new RuntimeException("Submission does not have associated Modrinth project.");
                    }
                } catch (Exception e) {
                    ModFestLog.error("[Events/event/setversion] Error fetching version data from Modrinth: ", e);
                    return event.reply("Error fetching version data from Modrinth: " + e.getMessage())
                            .withEphemeral(true);
                }
            } else if (event.getOption("updatedata").isPresent()) {
                var submissionId = event.getOption("updatedata")
                        .get()
                        .getOption("submission")
                        .flatMap(ApplicationCommandInteractionOption::getValue)
                        .get()
                        .asString();
                var submission = DataManager.getSubmission(submissionId);
                if (submission == null) {
                    return event.reply("Error: Could not find submission with ID " + submissionId).withEphemeral(true);
                }
                var error = DataManager.updateSubmissionData(submissionId);
                if (error != null) {
                    return event.reply("An error occurred fetching project data from Modrinth: " + error)
                            .withEphemeral(true);
                } else {
                    return event.reply("Updated the data of " + submission.name() + " successfully")
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
    public static Function<SelectMenuInteractionEvent, Publisher<Void>> ON_SELECT_MENU = event -> {
        if (event.getCustomId().equals("modfest-setversion-version-select-menu")) {
            var option = event.getValues().get(0);
            var split = option.split("/");
            var version = split[1];
            var error = DataManager.setVersion(split[0], version);
            if (error != null) {
                return event.reply("An error occurred: " + error).withEphemeral(true);
            }
            return event.reply("Updated version to " + version).withEphemeral(true);
        }
        return Mono.empty();
    };
    public static Function<ButtonInteractionEvent, Publisher<Void>> ON_BUTTON_INTERACTION = event -> {
        if (event.getCustomId().equals("modfest-registration-button")) {
            return Modals.REGISTER.onCommand(event);
        } else if (event.getCustomId().equals("modfest-submission-button")) {
            return Modals.SUBMIT.onCommand(event);
        }
        return Mono.empty();
    };

    public static Function<ChatInputAutoCompleteEvent, Publisher<Void>> ON_CHAT_AUTOCOMPLETE = event -> {
        String typing = event.getFocusedOption()
                .getValue()
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse("")
                .toLowerCase(Locale.ROOT);

        if (event.getCommandName().equals("event")) {
            if (event.getOption("unsubmit").isPresent() || event.getOption("setversion").isPresent() || event.getOption(
                    "updateversion").isPresent() || event.getOption("updatedata")
                    .isPresent() && event.getFocusedOption().getName().equals("submission")) {
                List<ApplicationCommandOptionChoiceData> suggestions = new ArrayList<>();

                var member = event.getInteraction().getMember();
                if (member.isPresent()) {
                    var user = DataManager.getUser(member.get());

                    user.getSubmissions()
                            .stream()
                            .filter(submission -> {
                                EventData activeEvent = ModFestPlatform.activeEvent;
                                return submission.getEvent().id().equals(activeEvent.id()) && activeEvent.phase()
                                        .canUpdateSubmission();
                            })
                            .forEach(submission -> {
                                if (typing.isEmpty() || submission.name()
                                        .toLowerCase(Locale.ROOT)
                                        .startsWith(typing.toLowerCase(Locale.ROOT)) || submission.id()
                                        .toLowerCase(Locale.ROOT)
                                        .startsWith(typing.toLowerCase(Locale.ROOT))) {
                                    suggestions.add(ApplicationCommandOptionChoiceData.builder()
                                            .name(submission.name())
                                            .value(submission.id())
                                            .build());
                                }
                            });

                    return event.respondWithSuggestions(suggestions);
                }
            }
        } else if (event.getCommandName().equals("admin") && event.getOption("setactiveevent").isPresent()) {
            return event.respondWithSuggestions(new ArrayList<>(DataManager.getEventList()
                    .stream()
                    .filter(eventData -> typing.isEmpty() || eventData.name()
                            .toLowerCase(Locale.ROOT)
                            .startsWith(typing))
                    .map(eventData -> ApplicationCommandOptionChoiceData.builder()
                            .name(eventData.name())
                            .value(eventData.id())
                            .build()).toList()));
        }

        return Mono.empty();
    };
}
