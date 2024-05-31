package net.modfest.platform.modrinth.project;

import com.google.gson.annotations.SerializedName;

public enum Side {
    @SerializedName("required") REQUIRED,
    @SerializedName("optional") OPTIONAL,
    @SerializedName("unsupported") UNSUPPORTED
}
