package net.modfest.platform.pojo;

import org.jspecify.annotations.Nullable;

public record SubmissionPatchData(
	@Nullable String name,
	@Nullable String description,
	@Nullable String sourceUrl,
	/**
	 * Should not be included if the submission isn't of type "other"
	 */
	@Nullable String homepage
) {
}
