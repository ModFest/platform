package net.modfest.platform;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.gateway.intent.IntentSet;
import io.javalin.Javalin;
import net.modfest.platform.data.DataManager;
import net.modfest.platform.data.StorageManager;
import net.modfest.platform.json.GsonMapper;
import net.modfest.platform.pojo.EventData;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModFestPlatform {
    private static final Logger LOGGER = Loggers.getLogger(ModFestPlatform.class);
    public static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .create();
    public static int port;
    public static File workingDir;

    public static EventData activeEvent = null;

    public static void main(String[] args) {
        port = 7069;

        Path workingDir = null;

        try {
            workingDir = Files.createDirectories(Path.of(System.getProperty("user.dir") + "/storage"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        ModFestPlatform.workingDir = new File(System.getProperty("user.dir") + "/storage");
        LOGGER.info("Current working directory is: %s", workingDir);
        StorageManager.init();

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new GsonMapper(GSON));
            config.enableCorsForAllOrigins();
        }).start(port);
        app.get("/events", ctx -> ctx.json(DataManager.getEventList()));
        app.get("/active_event", ctx -> ctx.json(DataManager.getActiveEvent()));
        app.get("/event/{id}", ctx -> {
            var id = ctx.pathParam("id");
            ctx.json(DataManager.getEvents().get(id));
        });
        app.get("/users", ctx -> ctx.json(DataManager.getUserList()));
        app.get("/user/{id}", ctx -> {
            var id = ctx.pathParam("id");
            ctx.json(DataManager.getUsers().get(id));
        });
        app.get("/badges", ctx -> ctx.json(DataManager.getBadgeList()));
        app.get("/badge/{id}", ctx -> {
            var id = ctx.pathParam("id");
            ctx.json(DataManager.getBadges().get(id));
        });
        app.get("/submissions", ctx -> ctx.json(DataManager.getSubmissionList()));
        app.get("/submission/{id}", ctx -> {
            var id = ctx.pathParam("id");
            ctx.json(DataManager.getSubmissions().get(id));
        });

        final String token = System.getenv("DISCORD_BOT_TOKEN");
        DiscordClient.create(token)
                .gateway()
                .setEnabledIntents(IntentSet.all())
                .withGateway(gateway -> gateway.onDisconnect()
                        .then()
                        .and(gateway.on(ReadyEvent.class, Events.ON_READY))
                        .and(gateway.on(MemberJoinEvent.class, Events.ON_MEMEBER_JOIN))
                        .and(gateway.on(ChatInputInteractionEvent.class, Events.ON_CHAT_INPUT_INTERACTION))
                        .and(gateway.on(ButtonInteractionEvent.class, Events.ON_BUTTON_INTERACTION))
                        .and(gateway.on(ModalSubmitInteractionEvent.class, Events.ON_MODAL_SUBMIT)))
                .block();
    }
}
