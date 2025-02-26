package net.modfest.platform.pojo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record SubmitRequestOther(
	@NonNull String name,
	@NonNull String description,
	@Nullable String homepage,
	@Nullable String sourceUrl,
	@Nullable String downloadUrl
) {
}
