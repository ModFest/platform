package net.modfest.platform.pojo;

import java.util.ArrayList;
import java.util.List;

public class SubmissionData {
    public String id;
    public String modrinthProjectId;
    public String name;
    public String description;
    public String teamName;
    public List<String> members = new ArrayList<>();
    public String iconUrl;
    public String galleryUrl;
    public String modrinthUrl;
    public String downloadUrl;
    public String sourceUrl;
    public boolean awarded;
    public String awardTitle = "";

    public SubmissionData(String modrinthProjectId) {
        this.modrinthProjectId = modrinthProjectId;
    }
}