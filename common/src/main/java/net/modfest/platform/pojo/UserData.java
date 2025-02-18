package net.modfest.platform.pojo;

import com.google.gson.annotations.SerializedName;
import lombok.With;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@With
public record UserData(
    String id,
	@Nullable String slug,
    @SerializedName(value = "name", alternate = {"display_name"})
    String name,
	@Nullable String pronouns,
    @Nullable String modrinthId,
	@Nullable String discordId,
	@Nullable String bio,
    @SerializedName(value = "icon", alternate = {"icon_url"})
	@Nullable String icon,
    Set<String> badges,
    Set<String> registered,
	@NonNull UserRole role
) implements Data {
	public UserData withRegistration(EventData event, boolean registered) {
		// Be careful to maintain immutability
		var newSet = new HashSet<>(this.registered());
		if (registered) {
			newSet.add(event.id());
		} else {
			newSet.remove(event.id());
		}
		return this.withRegistered(newSet);
	}
}
