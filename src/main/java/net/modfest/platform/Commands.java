package net.modfest.platform;

import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import net.modfest.platform.data.DataManager;

import java.util.List;

public class Commands {

    public static List<ApplicationCommandRequest> getGuildCommands() {
        ApplicationCommandRequest registerCommand = ApplicationCommandRequest.builder()
                .name("register")
                .description("Register for " + (DataManager.hasActiveEvent() ? DataManager.getActiveEvent().name : "ModFest"))
                .build();

        ApplicationCommandRequest fixmyrolesCommand = ApplicationCommandRequest.builder()
                .name("fixmyroles")
                .description("Corrects any errors with your roles. Use after re-joining the server.")
                .build();

        ApplicationCommandRequest eventCommand = ApplicationCommandRequest.builder()
                .name("event")
                .description("Unregister yourself from " + (DataManager.hasActiveEvent() ? DataManager.getActiveEvent().name : "ModFest"))
                .defaultPermission(false)
                .addOption(ApplicationCommandOptionData.builder()
                        .name("unregister")
                        .description("Unregister yourself from " + (DataManager.hasActiveEvent() ? DataManager.getActiveEvent().name : "ModFest"))
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                        .name("submit")
                        .description("Submit a mod for " + (DataManager.hasActiveEvent() ? DataManager.getActiveEvent().name : "ModFest"))
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                        .name("unsubmit")
                        .description("Retract a submission for " + (DataManager.hasActiveEvent() ? DataManager.getActiveEvent().name : "ModFest"))
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("submission")
                                .description("Select a submission to retract")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .autocomplete(true)
                                .required(true)
                                .build())
                        .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                        .name("setversion")
                        .description("Sets the version of your mod to a specific version on Modrinth")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("submission")
                                .description("Select a submission to set the version of")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .autocomplete(true)
                                .required(true)
                                .build())
                        .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                        .name("updateversion")
                        .description("Sets the version of your mod to the latest on Modrinth")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("submission")
                                .description("Select a submission to update the version of")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .autocomplete(true)
                                .required(true)
                                .build())
                        .build()
                )
                .addOption(ApplicationCommandOptionData.builder()
                        .name("updatedata")
                        .description("Updates your submission's data from Modrinth")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("submission")
                                .description("Select a submission to update the data of")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .autocomplete(true)
                                .required(true)
                                .build())
                        .build()
                )
                .build();

        ApplicationCommandRequest userCommand = ApplicationCommandRequest.builder()
                .name("user")
                .description("Make changes to your user account")
                .defaultPermission(false)
                .addOption(ApplicationCommandOptionData.builder()
                        .name("syncdata")
                        .description("Updates your profile with the latest information from Modrinth")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("set")
                        .description("Change information on your profile")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("name")
                                .description("Updates your display name to a new value")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .build())
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("pronouns")
                                .description("Updates your pronouns to a new value")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .build())
                        .build())
                .build();

        ApplicationCommandRequest adminCommand = ApplicationCommandRequest.builder()
                .name("admin")
                .description("ModFest's admin dashboard")
                .defaultPermission(false)
                .addOption(ApplicationCommandOptionData.builder()
                        .name("reload")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .description("Reload the ModFest Platform's data")
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("fixroles")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .description("Fixes any incorrect roles of a user on Discord")
                        .addOption(ApplicationCommandOptionData.builder()
                                .name("user")
                                .description("Fixes any incorrect roles of a user on Discord")
                                .type(ApplicationCommandOption.Type.USER.getValue())
                                .build())
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("sendregistrationmessage")
                        .description("Adds a registration message and button to this channel")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("sendsubmissionmessage")
                        .description("Adds a submission message and button to this channel")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .build())
//                .addOption(ApplicationCommandOptionData.builder()
//                        .name("setlogchannel")
//                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
//                        .description("Sets this channel as a log channel")
//                        .addOption(ApplicationCommandOptionData.builder()
//                                .name("info")
//                                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
//                                .description("Sets this channel as an info log channel")
//                                .build())
//                        .addOption(ApplicationCommandOptionData.builder()
//                                .name("debug")
//                                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
//                                .description("Sets this channel as a debug log channel")
//                                .build())
//                        .addOption(ApplicationCommandOptionData.builder()
//                                .name("error")
//                                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
//                                .description("Sets this channel as an error log channel")
//                                .build())
//                        .addOption(ApplicationCommandOptionData.builder()
//                                .name("lifecycle")
//                                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
//                                .description("Sets this channel as a lifecycle log channel")
//                                .build())
//                        .addOption(ApplicationCommandOptionData.builder()
//                                .name("all")
//                                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
//                                .description("Sets this channel as an all log channel")
//                                .build())
//                        .build())
                .build();

        return List.of(registerCommand, eventCommand, fixmyrolesCommand, userCommand, adminCommand);
    }

    public static List<ApplicationCommandRequest> getGlobalCommands() {
        return List.of();
    }
}
