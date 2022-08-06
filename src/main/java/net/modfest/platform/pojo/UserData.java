package net.modfest.platform.pojo;

import java.util.ArrayList;
import java.util.List;

public class UserData {
    public String id;
    public String displayName;
    public String pronouns;
    public String modrinthUserId;
    public String username;
    public String bio;
    public String iconUrl;
    public List<String> badges = new ArrayList<>();

    public UserData(String displayName, String pronouns, String modrinthUserId) {
        this.displayName = displayName;
        this.pronouns = pronouns;
        this.modrinthUserId = modrinthUserId;
    }
}
