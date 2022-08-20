package net.modfest.platform;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.interaction.*;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.gateway.intent.IntentSet;
import io.javalin.Javalin;
import io.javalin.http.Context;
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
            config.defaultContentType = "";
            config.jsonMapper(new GsonMapper(GSON));
            config.enableCorsForAllOrigins();
        }).start(PORT);
        app.get("/events", ctx -> json(ctx, DataManager.getEventList()));
        app.get("/active_event", ctx -> json(ctx, DataManager.getActiveEvent()));
        app.get("/event/{id}", ctx -> json(ctx, DataManager.getEvents().get(ctx.pathParam("id"))));
        app.get("/users", ctx -> json(ctx, DataManager.getUserList()));
        app.get("/user/{id}", ctx -> json(ctx, DataManager.getUsers().get(ctx.pathParam("id"))));
        app.get("/badges", ctx -> json(ctx, DataManager.getBadgeList()));
        app.get("/badge/{id}", ctx -> json(ctx, DataManager.getBadges().get(ctx.pathParam("id"))));
        app.get("/submissions", ctx -> json(ctx, DataManager.getSubmissionList()));
        app.get("/submission/{id}", ctx -> json(ctx, DataManager.getSubmissions().get(ctx.pathParam("id"))));

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
                        .and(gateway.on(ModalSubmitInteractionEvent.class, Events.ON_MODAL_SUBMIT))
                        .and(gateway.on(ChatInputAutoCompleteEvent.class, Events.ON_CHAT_AUTOCOMPLETE))
                        .and(gateway.on(SelectMenuInteractionEvent.class, Events.ON_SELECT_MENU))
                )
                .block();

        ModFestLog.close();
    }

    private static void json(Context ctx, Object object) {
        ctx.json(object).contentType("application/json;charset=utf-8");
    }
}
