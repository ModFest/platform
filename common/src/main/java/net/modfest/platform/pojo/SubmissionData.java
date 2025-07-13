package net.modfest.platform.pojo;

import com.google.gson.*;
import lombok.Getter;
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
							 @NonNull AssociatedData platform,
							 @Nullable String source,
							 @Nullable MarkerData markerData,
							 @Nullable WarpData warpData,
							 @Nullable BoothData boothData,
							 @NonNull Awards awards
	) implements Data {

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

	public record BoothData(
	                        int shards,
	                        int minutesToComplete,
	                        @NonNull BoothStatus status
	) {
		public enum BoothStatus {
			UNDER_CONSTRUCTION,
			PLAYABLE,
			COMPLETE
		}
	}

	public record WarpData(int x, int y, int z, @NonNull Direction direction) {
		@Getter
		public enum Direction {
			NORTH(180),
			NORTH_NORTH_EAST(-150),
			NORTH_EAST(-135),
			EAST_NORTH_EAST(-120),
			EAST(-90),
			EAST_SOUTH_EAST(-60),
			SOUTH_EAST(-45),
			SOUTH_SOUTH_EAST(-30),
			SOUTH(0),
			SOUTH_SOUTH_WEST(30),
			SOUTH_WEST(45),
			WEST_SOUTH_WEST(60),
			WEST(90),
			WEST_NORTH_WEST(120),
			NORTH_WEST(135),
			NORTH_NORTH_WEST(150);

			private final int yaw;

			Direction(int yaw) {
				this.yaw = yaw;
			}
		}
	}

	public record MarkerData(int x, int z, @NonNull String itemIcon) {
	}
}
