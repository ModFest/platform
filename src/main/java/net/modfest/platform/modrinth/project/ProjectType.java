package net.modfest.platform.modrinth.project;

import com.google.gson.annotations.SerializedName;

public enum ProjectType {
    @SerializedName("mod") MOD,
    @SerializedName("modpack") MODPACK
}
