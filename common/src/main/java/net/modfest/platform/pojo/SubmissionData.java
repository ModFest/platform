package net.modfest.platform.pojo;

import com.google.gson.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Set;

public record SubmissionData(@NonNull String id,
							 @NonNull String event,
							 @NonNull String name,
							 @NonNull String description,
							 @NonNull Set<String> authors,
							 @Nullable FileData platform,
							 @NonNull Images images,
							 @Nullable String download,
							 @Nullable String source,
							 @NonNull Awards awards
	) implements Data {

	public record Images(@Nullable String icon, @Nullable String screenshot) {
	}

	public record Awards(Set<String> theme, Set<String> extra) {
	}

	public record FileData(Object inner) {

		public record Modrinth(String projectId, String versionId) {
			public static final String KEY = "modrinth";
		}

		public record Github(String namespace, String repo) {
			public static final String KEY = "github";
		}

		public static class TypeAdapter implements JsonSerializer<FileData>, JsonDeserializer<FileData> {
			@Override
			public FileData deserialize(JsonElement json,
										Type typeOfT,
										JsonDeserializationContext context) throws JsonParseException {
				JsonObject jsonObject = json.getAsJsonObject();

				if (jsonObject == null) {
					return null;
				}

				var typeKey = jsonObject.remove("type").getAsString();

				return switch (typeKey) {
					case Modrinth.KEY -> new FileData(context.deserialize(jsonObject, Modrinth.class));
					case Github.KEY -> new FileData(context.deserialize(jsonObject, Github.class));
					default -> null;
				};
			}

			@Override
			public JsonElement serialize(FileData src,
										 Type typeOfSrc,
										 JsonSerializationContext context) {
				var jsonObj = context.serialize(src.inner).getAsJsonObject();

				var typeKey = switch (src.inner) {
					case Modrinth a -> Modrinth.KEY;
					case Github a -> Github.KEY;
					default -> throw new IllegalStateException();
				};
				jsonObj.addProperty("type", typeKey);
				return jsonObj;
			}
		}
	}
}
