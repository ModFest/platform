package net.modfest.platform.pojo;

import org.jspecify.annotations.Nullable;

public record UserPatchData(
	@Nullable String name,
	@Nullable String pronouns,
	@Nullable String bio
) {
}
