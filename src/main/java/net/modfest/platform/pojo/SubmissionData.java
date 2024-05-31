package net.modfest.platform.pojo;

import com.google.gson.*;
import net.modfest.platform.ModFestPlatform;
import net.modfest.platform.data.DataManager;
import reactor.util.annotation.Nullable;

import java.lang.reflect.Type;
import java.util.*;

public final class SubmissionData {
    private String id;
    private String name;
    private String description;
    private Set<String> authors;
    private Platform platform;
    private Images images;
    private String download;
    private String source;
    private Awards awards;

    private transient String event;

    public SubmissionData(String id,
                          String name,
                          String description,
                          Set<String> authors,
                          Platform platform,
                          Images images,
                          String download,
                          String source,
                          Awards awards) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.authors = authors;
        this.platform = platform;
        this.images = images;
        this.download = download;
        this.source = source;
        this.awards = awards;
    }

    public enum PlatformType {

        MODRINTH(Platform.Modrinth.class),
        GITHUB(Platform.Github.class),
        UNKNOWN(Platform.Unknown.class);

        final Class<?> type;

        PlatformType(Class<?> type) {
            this.type = type;
        }
    }



    public interface Platform {

        record Modrinth(String projectId, String versionId) implements Platform {
        }

        record Github(String namespace, String repo) implements Platform {
        }

        record Unknown() implements Platform {
        }

        class TypeAdapter implements JsonSerializer<Platform>, JsonDeserializer<Platform> {

            @Override
            public Platform deserialize(JsonElement json,
                                        Type typeOfT,
                                        JsonDeserializationContext context) throws JsonParseException {
                JsonObject jsonObject = json.getAsJsonObject();

                if (jsonObject == null) {
                    return new Unknown();
                }

                JsonElement platformType = jsonObject.remove("type");

                if (platformType == null) {
                    return new Unknown();
                }

                Optional<PlatformType> type = Arrays.stream(PlatformType.values())
                        .filter(value -> value.name().toLowerCase(Locale.ROOT).equals(platformType.getAsString()))
                        .findAny();

                if (type.isPresent()) {
                    return context.deserialize(jsonObject, type.get().type);
                }

                return new Unknown();
            }

            @Override
            public JsonElement serialize(Platform src,
                                         Type typeOfSrc,
                                         JsonSerializationContext context) {
                JsonObject platformObject = ModFestPlatform.RAW_GSON.toJsonTree(src).getAsJsonObject();

                Class<? extends Platform> platformClass = src.getClass();
                PlatformType platformType = Arrays.stream(PlatformType.values())
                        .filter(value -> value.type.equals(platformClass))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Could not find platform type for: " + src));

                JsonObject jsonObject = new JsonObject();
                jsonObject.add("type", context.serialize(platformType));

                for (String key : platformObject.keySet()) {
                    jsonObject.add(key, platformObject.get(key));
                }

                return jsonObject;
            }
        }
    }

    public record Images(@Nullable String icon, String screenshot) {
    }

    public Set<UserData> getAuthors() {
        return Set.of();
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Set<String> authors() {
        return authors;
    }

    public Platform platform() {
        return platform;
    }

    public Images images() {
        return images;
    }

    public String download() {
        return download;
    }

    public String source() {
        return source;
    }

    public Awards awards() {
        return awards;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAuthors(Set<String> authors) {
        this.authors = authors;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public void setImages(Images images) {
        this.images = images;
    }

    public void setDownload(String download) {
        this.download = download;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setAwards(Awards awards) {
        this.awards = awards;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SubmissionData) obj;
        return Objects.equals(this.id, that.id) && Objects.equals(this.name,
                that.name) && Objects.equals(this.description, that.description) && Objects.equals(this.authors,
                that.authors) && Objects.equals(this.platform, that.platform) && Objects.equals(this.platform,
                that.platform) && Objects.equals(this.images, that.images) && Objects.equals(this.download,
                that.download) && Objects.equals(this.source, that.source) && Objects.equals(this.awards, that.awards);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, authors, platform, platform, images, download, source, awards);
    }

    @Override
    public String toString() {
        return "SubmissionData[" + "id=" + id + ", " + "name=" + name + ", " + "description=" + description + ", " + "authors=" + authors + ", " + "type=" + platform + ", " + "typeData=" + platform + ", " + "images=" + images + ", " + "download=" + download + ", " + "source=" + source + ", " + "awards=" + awards + ']';
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public EventData getEvent() {
        return DataManager.getEvents().get(event);
    }

    public static class Awards {
        private Set<String> theme;
        private Set<String> extra;

        public Awards(Set<String> theme, Set<String> extra) {
            this.theme = theme;
            this.extra = extra;
        }

        public Set<String> theme() {
            return theme;
        }

        public Set<String> extra() {
            return extra;
        }
    }
}
