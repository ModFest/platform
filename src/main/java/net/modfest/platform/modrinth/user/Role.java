package net.modfest.platform.modrinth.user;

import com.google.gson.annotations.SerializedName;

public enum Role {
    @SerializedName("admin") ADMIN,
    @SerializedName("moderator") MODERATOR,
    @SerializedName("developer") DEVELOPER
}
