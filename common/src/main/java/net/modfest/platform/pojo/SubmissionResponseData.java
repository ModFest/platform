package net.modfest.platform.pojo;

import lombok.With;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

@With
public record SubmissionResponseData(@NonNull String id,
                                     @NonNull String event,
                                     @NonNull String name,
                                     @NonNull String description,
                                     @NonNull Set<String> authors,
                                     SubmissionData.@NonNull AssociatedData platform,
                                     @NonNull Images images,
                                     @Nullable String source,
                                     SubmissionData.@NonNull Awards awards
	) {

	public record Images(@Nullable String icon, @Nullable String screenshot) {
	}

	public static SubmissionResponseData fromData(SubmissionData data) {
		return new SubmissionResponseData(
			data.id(),
			data.event(),
			data.name(),
			data.description(),
			data.authors(),
			data.platform(),
			data.images(),
			data.source(),
			data.awards()
		);
	}
}
