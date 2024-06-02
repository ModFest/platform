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
import net.modfest.platform.json.EnumToLowerCaseJsonConverter;
import net.modfest.platform.json.GsonMapper;
import net.modfest.platform.log.ModFestLog;
import net.modfest.platform.pojo.EventData;
import net.modfest.platform.pojo.SubmissionData;
import net.modfest.platform.pojo.UserData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModFestPlatform {
    private static final GsonBuilder RAW_GSON_BUILDER = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeHierarchyAdapter(Enum.class, new EnumToLowerCaseJsonConverter())
            .setPrettyPrinting();
    public static final Gson RAW_GSON = RAW_GSON_BUILDER.create();
    public static final Gson GSON = RAW_GSON_BUILDER.registerTypeAdapter(EventData.DescriptionItem.class,
                    new EventData.DescriptionItem.TypeAdapter())
            .registerTypeAdapter(SubmissionData.Platform.class, new SubmissionData.Platform.TypeAdapter())
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
            logDir = Files.createDirectories(Path.of(System.getProperty("user.dir") + "/logs/" + FORMATTER.format(
                    Instant.now()).replaceAll("[^a-zA-Z\\d.-]", "_")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        ModFestPlatform.workingDir = workingDir.toFile();
        ModFestPlatform.logDir = logDir.toFile();

        ModFestLog.lifecycle("ModFestPlatform is starting...");

        ModFestLog.info("Current working directory is: %s", workingDir);
        StorageManager.init();
        StorageManager.loadAll();
        activeEvent = DataManager.getActiveEvent();

        Javalin app = Javalin.create(config -> {
            config.defaultContentType = "";
            config.jsonMapper(new GsonMapper(GSON));
            config.enableCorsForAllOrigins();
        }).start(PORT);
        app.get("/events", ctx -> {
            List<EventData> events = DataManager.getEventList();
            events.sort(Comparator.comparing(event -> event.dates().getFirst().start()));
            json(ctx, events);
        });
//        app.get("/active_event", ctx -> json(ctx, DataManager.getActiveEvent()));
        app.get("/event/{id}", ctx -> json(ctx, DataManager.getEvents().get(ctx.pathParam("id"))));
        app.get("/event/{id}/submissions", ctx -> json(ctx, DataManager.getSubmissions(ctx.pathParam("id")).values()));
        app.get("/users", ctx -> {
            if (ctx.queryParamMap().containsKey("map")) {
                json(ctx, DataManager.getUsers());
            } else {
                json(ctx, DataManager.getUserList());
            }
        });
        app.get("/user/{id}", ctx -> {
            UserData user = DataManager.getUsers().get(ctx.pathParam("id"));
            var json = GSON.toJsonTree(user).getAsJsonObject();
            var submissions = DataManager.getSubmissionList()
                    .stream()
                    .filter(submissionData -> submissionData.authors().contains(user.id()))
                    .toList();
            json.add("submissions", GSON.toJsonTree(submissions));
            json(ctx, json);
        });
        app.get("/badges", ctx -> {
            if (ctx.queryParamMap().containsKey("map")) {
                json(ctx, DataManager.getBadges());
            } else {
                json(ctx, DataManager.getBadgeList());
            }
        });
        app.get("/badge/{id}", ctx -> json(ctx, DataManager.getBadges().get(ctx.pathParam("id"))));
        app.get("/submissions", ctx -> {
            Map<String, Map<String, SubmissionData>> submissionsMap = DataManager.getSubmissions();
            Map<String, List<SubmissionData>> listMap = new HashMap<>();
            submissionsMap.forEach((event, submissions) -> listMap.put(event, submissions.values().stream().toList()));
            json(ctx, listMap);
        });
        app.get("/submission/{id}", ctx -> json(ctx, DataManager.getSubmissions().get(ctx.pathParam("id"))));

        final String token = "MTAwNzgwNzQ1MjkwMjg1ODgxMg.GfmW4a.gyEty8xfxk8fZlurbBvx7kS1TTl7sJoLItbFAk";
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
                        .and(gateway.on(SelectMenuInteractionEvent.class, Events.ON_SELECT_MENU)))
                .block();

        ModFestLog.close();
    }

    private static void json(Context ctx, Object object) {
        ctx.json(object).contentType("application/json;charset=utf-8");
    }
}
