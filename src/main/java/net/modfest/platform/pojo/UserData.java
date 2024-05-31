package net.modfest.platform.pojo;

import com.google.gson.annotations.SerializedName;
import net.modfest.platform.data.DataManager;

import java.util.List;
import java.util.Objects;
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
        this.registered = registered;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (UserData) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.slug, that.slug) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.pronouns, that.pronouns) &&
                Objects.equals(this.modrinthId, that.modrinthId) &&
                Objects.equals(this.discordId, that.discordId) &&
                Objects.equals(this.bio, that.bio) &&
                Objects.equals(this.icon, that.icon) &&
                Objects.equals(this.badges, that.badges) &&
                Objects.equals(this.registered, that.registered);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, slug, name, pronouns, modrinthId, discordId, bio, icon, badges, registered);
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
