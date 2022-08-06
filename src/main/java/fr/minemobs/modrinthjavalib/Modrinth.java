package fr.minemobs.modrinthjavalib;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.minemobs.modrinthjavalib.project.Project;
import fr.minemobs.modrinthjavalib.user.User;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;
import java.util.logging.Logger;

public class Modrinth {

    private static final Modrinth MODRINTH = new Modrinth("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");
    private final String baseURL;
    private final OkHttpClient client;
    private final Gson gson;
    private final Logger logger;
    private final String apiKey;

    public Modrinth(String apiKey) {
        this(apiKey, false);
    }

    public Modrinth(String apiKey, boolean stagingAPI) {
        this.client = new OkHttpClient.Builder().addInterceptor(chain ->
                chain.proceed(
                        chain.request()
                                .newBuilder()
                                .header("User-Agent", "ModFest Platform")
                                .build()
                )
        ).build();
        this.logger = Logger.getLogger("ModFest/ModrinthLib");
        this.apiKey = apiKey;
        this.gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        baseURL = stagingAPI ? "https://staging-api.modrinth.com/v2/" : "https://api.modrinth.com/v2/";
    }

    public static Project getProject(String id) throws IOException {
        return MODRINTH._getProject(id);
    }

    public static User getUser(String id) throws IOException {
        return MODRINTH._getUser(id);
    }

    public Project _getProject(String id) throws IOException {
        Project project = gson.fromJson(client.newCall(new Request.Builder().url(baseURL + "project/" + id)
                .addHeader("Authorization", apiKey)
                .build()).execute().body().string(), Project.class);
        project.setModrinth(this);
        return project;
    }

    public User _getUser(String id) throws IOException {
        var json = client.newCall(new Request.Builder().url(baseURL + "user/" + id).build())
                .execute()
                .body()
                .string();
        User user = gson.fromJson(json, User.class);
        user.setModrinth(this);
        return user;
    }

    public OkHttpClient getClient() {
        return client;
    }

    public Gson getGson() {
        return gson;
    }

    public Logger getLogger() {
        return logger;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public String getApiKey() {
        return apiKey;
    }
}
