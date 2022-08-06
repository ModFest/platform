package net.modfest.platform.data;

import com.google.gson.reflect.TypeToken;
import net.modfest.platform.ModFestPlatform;
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
        // Statically init everything
    }

    public static void loadAll() {
        STORES.forEach(Store::load);
    }

    public static void saveAll() {
        STORES.forEach(Store::save);
    }

    public static class Store<T> {
        private final File file;
        private final Consumer<T> onLoad;
        private final Type type;
        private T value;

        public Store(String name, T object, Type type) {
            this(name, object, type, null);
        }

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
            try (FileWriter writer = new FileWriter(file)) {
                ModFestPlatform.GSON.toJson(value, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void load() {
            try {
                this.value = ModFestPlatform.GSON.fromJson(new FileReader(file), type);
                if (onLoad != null) {
                    onLoad.accept(this.value);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        public void save() {
            try (FileWriter writer = new FileWriter(file)) {
                ModFestPlatform.GSON.toJson(this.value, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
