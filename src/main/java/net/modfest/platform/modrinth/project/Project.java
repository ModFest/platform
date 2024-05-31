package net.modfest.platform.modrinth.project;

import java.io.IOException;
import java.util.List;

import com.google.gson.annotations.SerializedName;
import net.modfest.platform.modrinth.Modrinth;
import net.modfest.platform.modrinth.project.version.Version;
import net.modfest.platform.pojo.SubmissionData;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.Nullable;

public class Project {

    public transient Modrinth modrinth;

    @SerializedName("wiki_url")
    public String wikiUrl;
    public String description;
    public String title;
    public String body;

    @SerializedName("discord_url")
    public String discordUrl;

    @SerializedName("source_url")
    public String sourceUrl;

    @SerializedName("body_url")
    public String bodyUrl;

    @SerializedName("moderator_message")
    public ModeratorMessage moderatorMessage;

    public int downloads;

    @SerializedName("donation_urls")
    public List<DonationUrlsItem> donationUrls;

    public String id;

    @SerializedName("server_side")
    public Side serverSide;

    public List<String> categories;
    public String slug;
    public List<GalleryItem> gallery;

    @SerializedName("icon_url")
    public String iconUrl;

    @SerializedName("project_type")
    public ProjectType projectType;

    public String team;
    public String published;

    @SerializedName("client_side")
    public Side clientSide;

    public License license;

    @SerializedName("issues_url")
    public String issuesUrl;

    public int followers;
    public List<String> versions;
    public String updated;
    public Status status;

    /**
     * Get all the versions of the project
     *
     * @return A list of versions
     * @throws IOException if the HTTP response code is 404
     */
    public Version[] getVersions() throws IOException {
        try (Response resp = modrinth.getClient()
                .newCall(new Request.Builder().url(modrinth.getBaseURL() + String.format("project/%s/version", this.id))
                        .build())
                .execute()) {
            if (resp.code() != 200) {
                throw new IOException("Failed to delete project. Response code: " + resp.code() + " with message: " + resp.body()
                        .string());
            }
            return modrinth.getGson().fromJson(resp.body().string(), Version[].class);
        }
    }

    /**
     * @param versionID The ID of the version to get
     * @return The version with the given ID
     * @throws IOException if the HTTP response code is not 200
     */
    public Version getVersion(String versionID) throws IOException {
        try (Response resp = modrinth.getClient()
                .newCall(new Request.Builder().url(modrinth.getBaseURL() + String.format("version/%s", versionID))
                        .build())
                .execute()) {
            if (resp.code() != 200) {
                throw new IOException("Failed to delete project. Response code: " + resp.code() + " with message: " + resp.body()
                        .string());
            }
            return modrinth.getGson().fromJson(resp.body().string(), Version.class);
        }
    }

    public Dependencies getDependencies() throws IOException {
        try (Response resp = modrinth.getClient()
                .newCall(new Request.Builder().url(modrinth.getBaseURL() + String.format("project/%s/dependencies",
                        this.id)).build())
                .execute()) {
            if (resp.code() != 200) {
                throw new IOException("Failed to delete project. Response code: " + resp.code() + " with message: " + resp.body()
                        .string());
            }
            return modrinth.getGson().fromJson(resp.body().string(), Dependencies.class);
        }
    }

    /**
     * @throws IOException if the HTTP response code is not equals to 204
     */
    public void deleteProject() throws IOException {
        try (Response resp = modrinth.getClient()
                .newCall(new Request.Builder().url(modrinth.getBaseURL() + "project/" + id)
                        .delete()
                        .addHeader("Authorization", modrinth.getApiKey())
                        .build())
                .execute()) {
            if (resp.code() != 204) {
                throw new IOException("Failed to delete project. Response code: " + resp.code() + " with message: " + resp.body()
                        .string());
            }
        }
    }

    /**
     * @return An optional link to the project's wiki page or other relevant information
     */
    @Nullable
    public String getWikiUrl() {
        return wikiUrl;
    }

    public String getDescription() {
        return description;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    /**
     * @return An optional invite link to the project's discord
     */
    @Nullable
    public String getDiscordUrl() {
        return discordUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    /**
     * @deprecated Only present for old projects
     */
    @Deprecated
    public Object getBodyUrl() {
        return bodyUrl;
    }

    /**
     * @return A message that a moderator sent regarding the project
     */
    @Nullable
    public ModeratorMessage getModeratorMessage() {
        return moderatorMessage;
    }

    public int getDownloads() {
        return downloads;
    }

    public List<DonationUrlsItem> getDonationUrls() {
        return donationUrls;
    }

    public String getId() {
        return id;
    }

    public Side getServerSide() {
        return serverSide;
    }

    public List<String> getCategories() {
        return categories;
    }

    /**
     * @return The slug of a project, used for vanity URLs
     */
    public String getSlug() {
        return slug;
    }

    public List<GalleryItem> getGallery() {
        return gallery;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

    public String getTeam() {
        return team;
    }

    public String getPublished() {
        return published;
    }

    public Side getClientSide() {
        return clientSide;
    }

    public License getLicense() {
        return license;
    }

    /**
     * @return An optional link to where to submit bugs or issues with the project
     */
    @Nullable
    public String getIssuesUrl() {
        return issuesUrl;
    }

    public int getFollowers() {
        return followers;
    }

    public List<String> getVersionIDs() {
        return versions;
    }

    public String getUpdated() {
        return updated;
    }

    public Status getStatus() {
        return status;
    }

    public void setModrinth(Modrinth modrinth) {
        if (this.modrinth != null || Thread.currentThread().getStackTrace()[1].getClassName()
                .equals(Modrinth.class.getName())) {
            return;
        }
        this.modrinth = modrinth;
    }

    public SubmissionData.Images getImages() {
        return new SubmissionData.Images(iconUrl,
                gallery.stream().filter(item -> item.featured).map(item -> item.url).findFirst().orElse(null));
    }
}