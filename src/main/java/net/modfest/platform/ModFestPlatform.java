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
import net.modfest.platform.log.ModFestLog;
import net.modfest.platform.pojo.EventData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ModFestPlatform {
    public static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .create();
    public static final int PORT = 7069;
    public static File workingDir;
    public static File logDir;

    public static EventData activeEvent = null;

    private static final String PATTERN_FORMAT = "yyy-MM-dd hh:mm:ss:SS";
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATTERN_FORMAT)
            .withZone(ZoneId.systemDefault());

    public static void main(String[] args) {
        Path workingDir = null;
        Path logDir = null;

        try {
            workingDir = Files.createDirectories(Path.of(System.getProperty("user.dir") + "/storage"));
            logDir = Files.createDirectories(Path.of(System.getProperty("user.dir") + "/logs/" + FORMATTER.format(Instant.now())
                    .replaceAll("[^a-zA-Z\\d.-]", "_")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        ModFestPlatform.workingDir = workingDir.toFile();
        ModFestPlatform.logDir = logDir.toFile();

        ModFestLog.lifecycle("ModFestPlatform is starting...");

        ModFestLog.info("Current working directory is: %s", workingDir);
        StorageManager.init();

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new GsonMapper(GSON));
            config.enableCorsForAllOrigins();
        }).start(PORT);
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

        ModFestLog.close();
    }
}
