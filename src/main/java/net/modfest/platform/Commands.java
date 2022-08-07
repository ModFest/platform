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
                .description("Register for " + DataManager.getActiveEvent().name)
                .build();

        ApplicationCommandRequest unregisterCommand = ApplicationCommandRequest.builder()
                .name("unregister")
                .defaultPermission(false)
                .description("Unregister yourself from " + DataManager.getActiveEvent().name)
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
                        .name("fixroles")
                        .description("Corrects any errors with your roles. Use after re-joining the server.")
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
                .description("ModFest's admin dashboard")
                .build();

        ApplicationCommandRequest adminCommand = ApplicationCommandRequest.builder()
                .name("admin")
                .defaultPermission(false)
                .addOption(ApplicationCommandOptionData.builder()
                        .name("reload")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .description("Reload the ModFest Platform's data")
                        .build())
//                .addOption(ApplicationCommandOptionData.builder()
//                        .name("fixallroles")
//                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
//                        .description("Fixes any incorrect roles on Discord")
//                        .build())
                .addOption(ApplicationCommandOptionData.builder()
                        .name("openregistration")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .description("Adds a registration message and button to this channel")
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
                .description("ModFest's admin dashboard")
                .build();

        return List.of(registerCommand, unregisterCommand, userCommand, adminCommand);
    }

    public static List<ApplicationCommandRequest> getGlobalCommands() {
        return List.of();
    }
}
