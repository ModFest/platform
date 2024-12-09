package net.modfest.platform.pojo;

import com.google.gson.annotations.SerializedName;

import java.util.Set;

public record UserData(
    String id,
    String slug,
    @SerializedName(value = "name", alternate = {"display_name"})
    String name,
    String pronouns,
    String modrinthId,
    String discordId,
    String bio,
    @SerializedName(value = "icon", alternate = {"icon_url"})
    String icon,
    Set<String> badges,
    Set<String> registered
) implements Data {
}
