package net.modfest.platform.pojo;

import java.util.HashMap;
import java.util.Map;

public class EventData {
    public String id;
    public String name;
    public boolean oneOff;
    public boolean archived;
    public boolean published;
    public String logo;
    public String backgroundColor;
    public String primaryColor;
    public String participantRoleId;
    public String awardRoleId;
    public Map<String, EventParticipantData> participants = new HashMap<>();
}
