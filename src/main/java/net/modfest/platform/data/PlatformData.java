package net.modfest.platform.data;

import net.modfest.platform.pojo.BadgeData;
import net.modfest.platform.pojo.EventData;
import net.modfest.platform.pojo.SubmissionData;
import net.modfest.platform.pojo.UserData;

import java.util.HashMap;
import java.util.Map;

public class PlatformData {
    public Map<String, UserData> users = new HashMap<>();
    public Map<String, EventData> events = new HashMap<>();
    public Map<String, SubmissionData> submissions = new HashMap<>();
    public Map<String, BadgeData> badges = new HashMap<>();

}
