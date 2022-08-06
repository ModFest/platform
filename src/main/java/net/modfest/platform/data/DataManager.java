package net.modfest.platform.data;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import fr.minemobs.modrinthjavalib.Modrinth;
import fr.minemobs.modrinthjavalib.user.User;
import net.modfest.platform.pojo.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DataManager {

    protected static ConfigData configData = new ConfigData();
    protected static PlatformData platformData = new PlatformData();

    public static String getGuildId() {
        return configData.guild;
    }

    public static Snowflake getUserRole() {
        return Snowflake.of(configData.userRoleId);
    }

    public static void addUserData(Snowflake id, UserData data) {
        platformData.users.put(id.asString(), data);
        StorageManager.USERS.save();
    }

    public static Map<String, EventData> getEvents() {
        return platformData.events;
    }

    public static List<EventData> getEventList() {
        return new ArrayList<>(platformData.events.values());
    }

    public static EventData getActiveEvent() {
        return platformData.events.get(configData.activeEvent);
    }

    public static Map<String, UserData> getUsers() {
        return platformData.users;
    }

    public static List<UserData> getUserList() {
        return new ArrayList<>(platformData.users.values());
    }

    public static Map<String, BadgeData> getBadges() {
        return platformData.badges;
    }

    public static List<BadgeData> getBadgeList() {
        return new ArrayList<>(platformData.badges.values());
    }

    public static Map<String, SubmissionData> getSubmissions() {
        return platformData.submissions;
    }

    public static List<SubmissionData> getSubmissionList() {
        return new ArrayList<>(platformData.submissions.values());
    }

    public static UserData getUserData(Snowflake id) {
        return platformData.users.get(id.asString());
    }

    public static Mono<Void> register(Member member, String eventId) {
        platformData.events.get(eventId).participants.put(member.getId().asString(), new EventParticipantData());
        StorageManager.EVENTS.save();
        return member.addRole(Snowflake.of(platformData.events.get(configData.activeEvent).participantRoleId))
                .and(member.addRole(getUserRole()));
    }

    public static Mono<Void> unregister(Member member, String eventId) {
        platformData.events.get(eventId).participants.remove(member.getId().asString());
        StorageManager.EVENTS.save();
        return member.removeRole(Snowflake.of(platformData.events.get(configData.activeEvent).participantRoleId));
    }

    public static boolean isRegistered(Snowflake id, String eventId) {
        return getEventParticipationData(id, eventId) != null;
    }

    public static EventParticipantData getEventParticipationData(Snowflake id, String eventId) {
        return platformData.events.get(eventId).participants.get(id.asString());
    }

    public static String updateUserData(Snowflake id) {
        var user = getUserData(id);
        try {
            updateUserData(id, Modrinth.getUser(user.modrinthUserId));
            return null;
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    public static void updateUserDisplayName(Snowflake id, String displayName) {
        var user = getUserData(id);
        user.displayName = displayName;
        StorageManager.USERS.save();
    }

    public static void updateUserPronouns(Snowflake id, String pronouns) {
        var user = getUserData(id);
        user.pronouns = pronouns;
        StorageManager.USERS.save();
    }

    public static void updateUserData(Snowflake id, User modrinthData) {
        var user = getUserData(id);
        user.username = modrinthData.username.toLowerCase(Locale.ROOT).replace(" ", "-");
        user.iconUrl = modrinthData.avatarUrl;
        user.bio = modrinthData.bio;
        StorageManager.USERS.save();
    }
}