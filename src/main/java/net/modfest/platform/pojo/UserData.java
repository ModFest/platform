package net.modfest.platform.pojo;

import com.google.gson.annotations.SerializedName;
import net.modfest.platform.data.DataManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class UserData {
    private String id;
    private String slug;
    @SerializedName(value = "name", alternate = {"display_name"})
    private String name;
    private String pronouns;
    private String modrinthId;
    private String discordId;
    private String bio;
    @SerializedName(value = "icon", alternate = {"icon_url"})
    private String icon;
    private Set<String> badges;
    private Set<String> registered;

    public UserData(
            String id,
            String slug,
            String name,
            String pronouns,
            String modrinthId,
            String discordId,
            String bio,

            String icon,
            Set<String> badges,

            Set<String> registered
    ) {
        this.id = id;
        this.slug = slug;
        this.name = name;
        this.pronouns = pronouns;
        this.modrinthId = modrinthId;
        this.discordId = discordId;
        this.bio = bio;
        this.icon = icon;
        this.badges = badges;
        this.registered = registered;

        if (this.registered == null) {
            this.registered = new HashSet<>();
        }
    }

    public List<SubmissionData> getSubmissions() {
        return DataManager.getSubmissionList()
                .stream()
                .filter(submission -> submission.authors().contains(id()))
                .toList();
    }

    public String id() {
        return id;
    }

    public String slug() {
        return slug;
    }

    @SerializedName(value = "name", alternate = {"display_name"})
    public String name() {
        return name;
    }

    public String pronouns() {
        return pronouns;
    }

    public String modrinthId() {
        return modrinthId;
    }

    public String discordId() {
        return discordId;
    }

    public String bio() {
        return bio;
    }

    @SerializedName(value = "icon", alternate = {"icon_url"})
    public String icon() {
        return icon;
    }

    public Set<String> badges() {
        return badges;
    }

    public Set<String> registered() {
        if(registered == null) {
            registered = new HashSet<>();
        }
        return registered;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPronouns(String pronouns) {
        this.pronouns = pronouns;
    }

    public void setModrinthId(String modrinthId) {
        this.modrinthId = modrinthId;
    }

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setBadges(Set<String> badges) {
        this.badges = badges;
    }

    public void setRegistered(Set<String> registered) {
        if (registered == null) {
            registered = new HashSet<>();
        }
        this.registered = registered;
    }

    @Override
    public String toString() {
        return "UserData[" +
                "id=" + id + ", " +
                "slug=" + slug + ", " +
                "name=" + name + ", " +
                "pronouns=" + pronouns + ", " +
                "modrinthId=" + modrinthId + ", " +
                "discordId=" + discordId + ", " +
                "bio=" + bio + ", " +
                "icon=" + icon + ", " +
                "badges=" + badges + ", " +
                "registered=" + registered + ']';
    }

}
