package net.modfest.platform.pojo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public record SubmitRequestOther(
	@NonNull String name,
	@NonNull String description,
	@NonNull Set<String> authors,
	@Nullable String homepage,
	@Nullable String sourceUrl,
	@Nullable String downloadUrl
) {
}
