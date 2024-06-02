package net.modfest.platform.data;

import net.modfest.platform.ModFestPlatform;
import net.modfest.platform.log.ModFestLog;
import net.modfest.platform.modrinth.Modrinth;
import net.modfest.platform.modrinth.project.Project;
import net.modfest.platform.pojo.*;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StorageManager {

    public static final Store<ConfigData> CONFIG = new Store<>("config",
            DataManager.configData,
            ConfigData.class,
            data -> DataManager.configData = data);
    private static final List<Store<?>> STORES = new ArrayList<>();

    static {
        STORES.add(CONFIG);

//        STORES.add(BADGES);
//        STORES.add(EVENTS);
//        STORES.add(SUBMISSIONS);
//        STORES.add(USERS);
//
//        STORES.add(BADGES_V2);
//        STORES.add(EVENTS_V2);
//        STORES.add(SUBMISSIONS_V2);
//        STORES.add(USERS_V2);
    }

    public static void init() {
        ModFestLog.lifecycle("[StorageManager] Initializing stores...");
        // Statically init everything
    }

    public static void main(String[] args) {
        loadAll();
    }

    public static void loadAll() {
        ModFestLog.lifecycle("[StorageManager] Loading all stores");
        STORES.forEach(Store::load);

        loadUsers();
        loadEvents();
        loadBadges();
        loadSubmissions();

        // Migrate/fix legacy data
//        var users = DataManager.getUsers();
//        DataManager.getSubmissions().values().forEach(submissions -> submissions.values().forEach(submission -> {
//            Set<String> newAuthors = submission.authors().stream().map(authorName -> {
//                String transformedName = authorName.toLowerCase();
//                if (!authorName.equals(transformedName) && users.containsKey(transformedName)) {
//                    System.out.println("Found user for legacy author: " + authorName + " -> " + transformedName);
//                    return transformedName;
//                }
//                return authorName;
//            }).collect(Collectors.toSet());
//            submission.setAuthors(newAuthors);
//
//            String icon = submission.images().icon();
//            String screenshot = submission.images()
//                    .screenshot();
//            if (icon == null || is404(icon) || screenshot == null || is404(screenshot)) {
//                switch (submission.platform()) {
//                    case SubmissionData.Platform.Modrinth(String projectId, String versionId) -> {
//                        try {
//                            Project modrinthProject = Modrinth.getProject(projectId);
//                            SubmissionData.Images modrinthImages = modrinthProject.getImages();
//                            submission.setImages(new SubmissionData.Images(icon != null && !is404(icon) ? icon : modrinthImages.icon(),
//                                    screenshot != null && !is404(screenshot) ? screenshot : modrinthImages.screenshot()));
//                        } catch (IOException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
//                    default -> {
//                    }
//                }
//            }
//        }));
//        saveSubmissions();
    }

    private static boolean is404(String urlString) {
        if (urlString.startsWith("/")) { // ignore local file
            return false;
        }
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            return connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static void loadSubmissions() {
        ModFestLog.info("[StorageManager/Submissions] Loading submissions...");
        Map<String, List<SubmissionData>> events = new HashMap<>();
        Path path = Path.of(ModFestPlatform.workingDir.getPath(), "submissions");
        try (var files = Files.newDirectoryStream(path)) {
            for (var file : files) {
                if (Files.isDirectory(file)) {
                    if (DataManager.platformData.events.containsKey(file.getFileName().toString())) {
                        loadSubmissionsFromDirectory(file, events);
                    } else {
                        ModFestLog.info("[StorageManager/Submissions] Submission directory for unknown event: " + file);
                    }
                } else {
                    ModFestLog.info(
                            "[StorageManager/Submissions] Error loading submissions, non-directory in submissions directory: " + file);
                }
            }
        } catch (IOException e) {
            ModFestLog.info("[StorageManager/Submissions] Error loading submissions", e);
        }
        ModFestLog.info("[StorageManager/Submissions] Submissions loaded!");
        events.forEach((event, submissions) -> {
            submissions.forEach(submissionData -> submissionData.setEvent(event));
            DataManager.platformData.submissions.put(event,
                    submissions.stream().collect(Collectors.toMap(SubmissionData::id, submission -> submission)));
        });
    }

    private static void loadSubmissionsFromDirectory(Path dir, Map<String, List<SubmissionData>> events) {
        List<SubmissionData> submissions = new ArrayList<>();
        events.put(dir.getFileName().toString(), submissions);
        try (var files = Files.newDirectoryStream(dir)) {
            for (Path file : files) {
                if (Files.isRegularFile(file)) {
                    loadDataFromFile(file,
                            submissions,
                            (Class<SubmissionData>) (Object) SubmissionData.class,
                            "[StorageManager/Submissions]");
                    SubmissionData submission = submissions.getLast();
                    if (submission.id() == null) {
                        String fileName = file.getFileName().toString();
                        if (fileName.endsWith(".json")) {
                            fileName = fileName.substring(0, fileName.length() - 5);
                        }
                        submission.setId(fileName);
                    }
                }
            }
        } catch (IOException e) {
            ModFestLog.info("[StorageManager/Submissions] Error loading submissions from " + dir + "; " + e);
        }
    }

    private static void loadEvents() {
        List<EventData> events = new ArrayList<>();
        try (var files = Files.newDirectoryStream(Path.of(ModFestPlatform.workingDir.getPath(), "events"))) {
            for (Path file : files) {
                if (Files.isRegularFile(file)) {
                    loadDataFromFile(file, events, EventData.class, "[StorageManager/Events]");
                }
            }
        } catch (IOException e) {
            ModFestLog.info("[StorageManager/Submissions] Error loading events; " + e);
        }
        events.forEach(event -> DataManager.platformData.events.put(event.id(), event));
    }

    private static void loadUsers() {
        List<UserData> users = new ArrayList<>();
        try (var files = Files.newDirectoryStream(Path.of(ModFestPlatform.workingDir.getPath(), "users"))) {
            for (Path file : files) {
                if (Files.isRegularFile(file)) {
                    loadDataFromFile(file, users, UserData.class, "[StorageManager/Users]");
                }
            }
        } catch (IOException e) {
            ModFestLog.info("[StorageManager/Users] Error loading users; " + e);
        }
        users.forEach(user -> DataManager.platformData.users.put(user.id(), user));
    }

    private static void loadBadges() {
        List<BadgeData> badges = new ArrayList<>();
        try (var files = Files.newDirectoryStream(Path.of(ModFestPlatform.workingDir.getPath(), "badges"))) {
            for (Path file : files) {
                if (Files.isRegularFile(file)) {
                    loadDataFromFile(file, badges, BadgeData.class, "[StorageManager/Badges]");
                }
            }
        } catch (IOException e) {
            ModFestLog.info("[StorageManager/Badges] Error loading badges; " + e);
        }
        badges.forEach(badge -> DataManager.platformData.badges.put(badge.id(), badge));
    }

    private static <T> void loadDataFromFile(Path file, List<T> dataList, Class<T> type, String logPrefix) {
        ModFestLog.info(logPrefix + " Loading " + file);
        try {
            T data = ModFestPlatform.GSON.fromJson(new FileReader(file.toFile()), type);
            dataList.add(data);
        } catch (Throwable e) {
            ModFestLog.info(logPrefix + " Error loading " + file + "; " + e);
        }
    }


    public static String uniqueify(String name, Function<String, Boolean> isTaken) {
        return uniqueifyInternal(name.toLowerCase(Locale.ROOT).replaceAll("[^a-zA-Z0-9]", "_"), isTaken);
    }

    public static String uniqueifyInternal(String name, Function<String, Boolean> isTaken) {
        if (isTaken.apply(name)) {
            return uniqueify(name + "_", isTaken);
        } else {
            return name;
        }
    }

    public static void saveAll() {
        ModFestLog.lifecycle("[StorageManager] Saving all...");
        STORES.forEach(Store::save);
        saveSubmissions();
        saveEvents();
        saveUsers();
        saveBadges();
    }

    private static void saveSubmissions() {
        DataManager.platformData.submissions.forEach((eventId, submissions) -> submissions.values()
                .forEach(submission -> saveSubmission(eventId, submission)));
    }

    public static void saveSubmission(String eventId, String submissionId) {
        saveSubmission(eventId, DataManager.platformData.submissions.get(eventId).get(submissionId));
    }

    public static void saveSubmission(String eventId, SubmissionData submission) {
        String fileName = submission.id() + ".json";
        DataManager.platformData.submissions.get(eventId).put(submission.id(), submission);
        Path filePath = Path.of(ModFestPlatform.workingDir.getPath(), "submissions", eventId, fileName);
        saveDataToFile(filePath, submission, "[StorageManager/Submissions]");
    }

    public static void saveEvents() {
        DataManager.platformData.events.forEach((eventId, event) -> {
            String fileName = eventId + ".json";
            Path filePath = Path.of(ModFestPlatform.workingDir.getPath(), "events", fileName);
            saveDataToFile(filePath, event, "[StorageManager/Events]");
        });
    }

    private static void saveUsers() {
        DataManager.platformData.users.forEach((userId, user) -> saveUser(user));
    }


    public static void saveUser(String userId) {
        saveUser(DataManager.platformData.users.get(userId));
    }


    public static void saveUser(UserData user) {
        String fileName = user.id() + ".json";
        Path filePath = Path.of(ModFestPlatform.workingDir.getPath(), "users", fileName);
        DataManager.platformData.users.put(user.id(), user);
        saveDataToFile(filePath, user, "[StorageManager/Users]");
    }

    private static void saveBadges() {
        DataManager.platformData.badges.forEach((badgeId, badge) -> {
            String fileName = badgeId + ".json";
            Path filePath = Path.of(ModFestPlatform.workingDir.getPath(), "badges", fileName);
            saveDataToFile(filePath, badge, "[StorageManager/Badges]");
        });
    }

    public static void saveConfig() {
        CONFIG.save();
    }

    private static <T> void saveDataToFile(Path file, T data, String logPrefix) {
        try {
            Files.createDirectories(file.getParent());
            ModFestLog.info(logPrefix + " Saving " + file);
            try (FileWriter writer = new FileWriter(file.toFile())) {
                ModFestPlatform.GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            ModFestLog.info(logPrefix + " Error saving " + file + "; " + e);
            e.printStackTrace();
        }
    }


    public static class Store<T> {
        private final File file;
        private final Consumer<T> onLoad;
        private final Type type;
        private T value;

        public Store(String name, T defaultValue, Type type, Consumer<T> onLoad) {
            this.file = new File(ModFestPlatform.workingDir, name + ".json");
            this.value = defaultValue;
            this.onLoad = onLoad;
            this.type = type;

            if (file.exists()) {
                load();
                save();
            } else {
                initFile();
            }
        }

        private void initFile() {
            ModFestLog.info("[StorageManager/Store] Initializing " + file.getName());
            try (FileWriter writer = new FileWriter(file)) {
                ModFestPlatform.GSON.toJson(value, writer);
            } catch (IOException e) {
                ModFestLog.info("[StorageManager/Store] Error initializing " + file.getName() + "; " + e);
                e.printStackTrace();
            }
        }

        public void load() {
            ModFestLog.info("[StorageManager/Store] Loading " + file.getName());
            try {
                this.value = ModFestPlatform.GSON.fromJson(new FileReader(file), type);
                if (onLoad != null) {
                    onLoad.accept(this.value);
                }
            } catch (FileNotFoundException e) {
                ModFestLog.info("[StorageManager/Store] Error loading " + file.getName() + "; " + e);
                e.printStackTrace();
            }
        }

        public void save() {
            ModFestLog.info("[StorageManager/Store] Saving " + file.getName());
            try (FileWriter writer = new FileWriter(file)) {
                ModFestPlatform.GSON.toJson(this.value, writer);
            } catch (IOException e) {
                ModFestLog.info("[StorageManager/Store] Error saving " + file.getName() + "; " + e);
                e.printStackTrace();
            }
        }
    }
}
