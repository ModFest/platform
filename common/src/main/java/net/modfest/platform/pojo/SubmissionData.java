package net.modfest.platform.pojo;

import com.google.gson.JsonObject;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public record SubmissionData(@NonNull String id,
							 @NonNull String event,
							 @NonNull String name,
							 @NonNull String description,
							 @NonNull Set<String> authors,
							 @NonNull JsonObject platform, // TODO
							 @NonNull Images images,
							 @NonNull String download,
							 @NonNull String source,
	 						 @NonNull Awards awards
	) implements Data {

	public record Images(@Nullable String icon, String screenshot) {
	}

	public record Awards(Set<String> theme, Set<String> extra) {
	}
}
