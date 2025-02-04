package net.modfest.platform.pojo;

import com.google.gson.JsonElement;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public record SubmissionData(@NonNull String id,
							 @NonNull String event,
							 @NonNull String name,
							 @NonNull String description,
							 @NonNull Set<String> authors,
							 @NonNull JsonElement platform, // TODO
							 @NonNull Images images,
							 @NonNull String download,
							 @Nullable String source,
							 @NonNull Awards awards
	) implements Data {

	public record Images(@Nullable String icon, @Nullable String screenshot) {
	}

	public record Awards(Set<String> theme, Set<String> extra) {
	}
}
