package net.modfest.platform.pojo;

import java.util.ArrayList;
import java.util.List;

public class SubmissionData {
    public String id;
    public String slug;
    public String name;
    public String summary;
    public String description;
    public String teamName = "";
    public List<String> members = new ArrayList<>();
    public String iconUrl;
    public String galleryUrl;
    public String modrinthUrl;
    public String versionId;
    public String downloadUrl;
    public String sourceUrl;
    public boolean awarded = false;
    public String awardTitle = "";

    public SubmissionData(String id, String slug, String name, String summary, String description, String author, String iconUrl, String galleryUrl, String modrinthUrl, String versionId, String downloadUrl, String sourceUrl) {
        this.id = id;
        this.slug = slug;
        this.name = name;
        this.summary = summary;
        this.description = description;
        this.iconUrl = iconUrl;
        this.galleryUrl = galleryUrl;
        this.modrinthUrl = modrinthUrl;
        this.versionId = versionId;
        this.downloadUrl = downloadUrl;
        this.sourceUrl = sourceUrl;
        this.members.add(author);
    }

    @Override
    public String toString() {
        return "SubmissionData{" +
                "id='" + id + '\'' +
                ", slug='" + slug + '\'' +
                ", name='" + name + '\'' +
                ", summary='" + summary + '\'' +
                ", description='" + description + '\'' +
                ", teamName='" + teamName + '\'' +
                ", members=" + members +
                ", iconUrl='" + iconUrl + '\'' +
                ", galleryUrl='" + galleryUrl + '\'' +
                ", modrinthUrl='" + modrinthUrl + '\'' +
                ", versionId='" + versionId + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", sourceUrl='" + sourceUrl + '\'' +
                ", awarded=" + awarded +
                ", awardTitle='" + awardTitle + '\'' +
                '}';
    }
}