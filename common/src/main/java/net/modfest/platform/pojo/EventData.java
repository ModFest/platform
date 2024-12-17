package net.modfest.platform.pojo;

import com.google.gson.*;
import org.jspecify.annotations.NonNull;

import java.util.Date;
import java.util.List;

public record EventData(@NonNull String id,
                        @NonNull String name,
                        @NonNull String subtitle,
                        @NonNull Phase phase,
                        @NonNull List<DateRange> dates,
                        @NonNull Images images,
                        @NonNull Colors colors,
                        @NonNull DiscordRoles discordRoles,
                        @NonNull String mod_loader,
                        @NonNull String minecraft_version,
                        @NonNull String modpack,
                        @NonNull List<DescriptionItem<?>> description) implements Data {
    public enum Type {
        MODFEST,
        BLANKETCON
    }

    public enum Phase {
        PLANNING(true, false, false),
        MODDING(true, true, true),
        BUILDING(false, false, true),
        SHOWCASE(false, false, false),
        COMPLETE(false, false, false);

        private final boolean registrations;
        private final boolean submissions;
        private final boolean updates;

        Phase(boolean registrations, boolean submissions, boolean updates) {
            this.registrations = registrations;
            this.submissions = submissions;
            this.updates = updates;
        }

        public boolean canRegister() {
            return registrations;
        }

        public boolean canSubmit() {
            return submissions;
        }

        public boolean canUpdateSubmission() {
            return updates;
        }
    }

    public record Images(String full, String transparent, String wordmark, String background) {
    }

    public record Colors(String primary, String secondary) {
    }

    public record DiscordRoles(String participant, String award) {
    }

    public record DescriptionItem<T>(T content) {
        public static final String CONTENT_KEY = "content";

        public record Markdown(String markdown) {
            public static final String KEY = "markdown";
        }

        public static class TypeAdapter implements JsonSerializer<DescriptionItem<?>>, JsonDeserializer<DescriptionItem<?>> {
            public static final String TYPE_KEY = "type";

            @Override
            public DescriptionItem<?> deserialize(JsonElement json,
                                                  java.lang.reflect.Type typeOfT,
                                                  JsonDeserializationContext context) throws JsonParseException {
                JsonObject jsonObject = json.getAsJsonObject();
                String type = jsonObject.get(TYPE_KEY).getAsString();
                JsonElement content = jsonObject.get(CONTENT_KEY);

                return switch (type) {
                    case Markdown.KEY -> new DescriptionItem<Markdown>(context.deserialize(content, Markdown.class));
                    default -> throw new JsonParseException("Invalid DescriptionItem type: " + type);
                };
            }

            @Override
            public JsonElement serialize(DescriptionItem<?> src,
                                         java.lang.reflect.Type typeOfSrc,
                                         JsonSerializationContext context) {
                var json = new JsonObject();
                json.add(TYPE_KEY, new JsonPrimitive(switch (src.content) {
                    case Markdown ignored -> Markdown.KEY;
                    default -> throw new IllegalStateException("Unexpected value: " + src);
                }));
                json.add(CONTENT_KEY, context.serialize(src.content));
                return json;
            }
        }
    }

    public record DateRange(String name, String description, Phase phase, Date start, Date end) {
    }
}
