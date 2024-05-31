package net.modfest.platform.modrinth.project.version;

import com.google.gson.annotations.SerializedName;

public enum VersionType {
    @SerializedName("release") RELEASE,
    @SerializedName("beta") BETA,
    @SerializedName("alpha") ALPHA
}
