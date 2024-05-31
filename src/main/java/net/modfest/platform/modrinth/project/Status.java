package net.modfest.platform.modrinth.project;

import com.google.gson.annotations.SerializedName;

public enum Status {
    @SerializedName("approved") APPROVED,
    @SerializedName("rejected") REJECTED,
    @SerializedName("draft") DRAFT,
    @SerializedName("unlisted") UNLISTED,
    @SerializedName("processing") PROCESSING,
    @SerializedName("unknown") UNKNOWN
}
