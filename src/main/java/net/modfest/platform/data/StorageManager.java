package net.modfest.platform.data;

import com.google.gson.reflect.TypeToken;
import net.modfest.platform.ModFestPlatform;
import net.modfest.platform.log.ModFestLog;
import net.modfest.platform.pojo.*;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class StorageManager {

    public static final Store<ConfigData> CONFIG = new Store<>("config", DataManager.configData, ConfigData.class, data -> DataManager.configData = data);
    public static final Store<Map<String, BadgeData>> BADGES = new Store<>("badges", DataManager.platformData.badges, new TypeToken<Map<String, BadgeData>>() {
    }.getType(), data -> DataManager.platformData.badges = data);
    public static final Store<Map<String, EventData>> EVENTS = new Store<>("events", DataManager.platformData.events, new TypeToken<Map<String, EventData>>() {
    }.getType(), data -> {
        DataManager.platformData.events = data;
        ModFestPlatform.activeEvent = data.get(DataManager.configData.activeEvent);
    });
    public static final Store<Map<String, SubmissionData>> SUBMISSIONS = new Store<>("submissions", DataManager.platformData.submissions, new TypeToken<Map<String, SubmissionData>>() {
    }.getType(), data -> DataManager.platformData.submissions = data);
    public static final Store<Map<String, UserData>> USERS = new Store<>("users", DataManager.platformData.users, new TypeToken<Map<String, UserData>>() {
    }.getType(), data -> DataManager.platformData.users = data);

    private static final List<Store<?>> STORES = new ArrayList<>();

    static {
        STORES.add(CONFIG);

        STORES.add(BADGES);
        STORES.add(EVENTS);
        STORES.add(SUBMISSIONS);
        STORES.add(USERS);
    }

    public static void init() {
        ModFestLog.lifecycle("[StorageManager] Initializing stores...");
        // Statically init everything
    }

    public static void loadAll() {
        ModFestLog.lifecycle("[StorageManager] Loading all stores");
        STORES.forEach(Store::load);
    }

    public static void saveAll() {
        ModFestLog.lifecycle("[StorageManager] Saving all stores");
        STORES.forEach(Store::save);
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
