package net.modfest.platform.pojo;

import com.google.gson.*;
import lombok.With;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Set;

@With
public record SubmissionData(@NonNull String id,
							 @NonNull String event,
							 @NonNull String name,
							 @NonNull String description,
							 @NonNull Set<String> authors,
							 SubmissionData.@NonNull AssociatedData platform,
							 @NonNull Images images,
							 @Nullable String source,
							 @NonNull Awards awards
	) implements Data {

	public record Images(@Nullable String icon, @Nullable String screenshot) {
	}

	public record Awards(Set<String> theme, Set<String> extra) {
	}

	/**
	 * <ul>
	 *     <li>Modrinth mod: Contains a {@link Modrinth} object with a version id. Url is based on the project id, and the version id is used for inclusion in the pack</li>
	 *     <li>Modrinth non-project: Contains a {@link Modrinth} object but without a version id. Not included in the pack</li>
	 *     <li>Other mod: Contains a {@link Other} object. If it has a {@link Other#downloadUrl} then it's included in the pack</li>
	 * </ul>
	 */
	public record AssociatedData(Object inner) {
		public record Modrinth(String projectId, @Nullable String versionId) {
			public static final String KEY = "modrinth";
		}

		@With
		public record Other(@Nullable String homepageUrl, @Nullable String downloadUrl) {
			public static final String KEY = "other";
		}

		public static class TypeAdapter implements JsonSerializer<AssociatedData>, JsonDeserializer<AssociatedData> {
			@Override
			public AssociatedData deserialize(JsonElement json,
											  Type typeOfT,
											  JsonDeserializationContext context) throws JsonParseException {
				JsonObject jsonObject = json.getAsJsonObject();

				if (jsonObject == null) {
					return null;
				}

				var typeKey = jsonObject.remove("type").getAsString();

				return switch (typeKey) {
					case Modrinth.KEY -> new AssociatedData(context.deserialize(jsonObject, Modrinth.class));
					case Other.KEY -> new AssociatedData(context.deserialize(jsonObject, Other.class));
					default -> null;
				};
			}

			@Override
			public JsonElement serialize(AssociatedData src,
										 Type typeOfSrc,
										 JsonSerializationContext context) {
				var jsonObj = context.serialize(src.inner).getAsJsonObject();

				var typeKey = switch (src.inner) {
					case Modrinth a -> Modrinth.KEY;
					case Other a -> Other.KEY;
					default -> throw new IllegalStateException();
				};
				jsonObj.addProperty("type", typeKey);
				return jsonObj;
			}
		}
	}
}
