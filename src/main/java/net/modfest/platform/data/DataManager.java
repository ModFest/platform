package net.modfest.platform.data;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import fr.minemobs.modrinthjavalib.Modrinth;
import fr.minemobs.modrinthjavalib.project.Project;
import fr.minemobs.modrinthjavalib.project.version.FilesItem;
import fr.minemobs.modrinthjavalib.user.User;
import net.modfest.platform.log.ModFestLog;
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
        ModFestLog.debug("[DataManager] Adding new user: " + data);
        platformData.users.put(id.asString(), data);
        StorageManager.USERS.save();
    }

    public static void addSubmission(Snowflake userSnowflake, SubmissionData newSubmission, String event) {
        ModFestLog.debug("[DataManager] Adding new submission: %s", newSubmission.toString());
        var existingSubmission = platformData.submissions.get(newSubmission.id);
        var userId = userSnowflake.asString();
        if (existingSubmission == null) {
            platformData.submissions.put(newSubmission.id, newSubmission);
        } else if (!existingSubmission.members.contains(userId)) {
            existingSubmission.members.add(userId);
        }
        var submissions = platformData.events.get(event).participants.get(userId).submissions;
        if (!submissions.contains(newSubmission.id)) {
            submissions.add(newSubmission.id);
        }
        StorageManager.SUBMISSIONS.save();
        StorageManager.EVENTS.save();
    }

    public static String updateSubmissionData(String submissionId) {
        try {
            updateSubmissionData(submissionId, Modrinth.getProject(submissionId));
            return null;
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    public static String setVersion(String submissionId, String versionId) {
        try {
            var submission = getSubmissions().get(submissionId);
            var version = Modrinth.getProject(submissionId).getVersion(versionId);
            submission.versionId = versionId;
            submission.downloadUrl = version.getFiles().stream().filter(FilesItem::isPrimary).findFirst().get().url;
            return null;
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    public static void updateSubmissionData(String submissionId, Project modrinthData) {
        var submission = getSubmissions().get(submissionId);
        submission.slug = modrinthData.slug;
        submission.name = modrinthData.title;
        submission.summary = modrinthData.description;
        submission.description = modrinthData.body;
        submission.iconUrl = modrinthData.iconUrl;
        var primaryGallery = modrinthData.gallery.stream().filter(item -> item.featured).findFirst();
        primaryGallery.ifPresent(galleryItem -> submission.galleryUrl = galleryItem.url);
        submission.sourceUrl = modrinthData.sourceUrl;
        StorageManager.SUBMISSIONS.save();
    }

    public static void removeSubmission(Snowflake userId, String submissionId, String event) {
        var submissions = platformData.events.get(event).participants.get(userId.asString()).submissions;
        submissions.remove(submissionId);
        var members = platformData.submissions.get(submissionId).members;
        members.remove(userId.asString());
        ModFestLog.debug("[DataManager] Removed submission '%s' from user '%s' for event '%s'", submissionId, userId.asString(), event);

        if (members.isEmpty()) {
            platformData.events.forEach((eventId, eventData) -> {
                eventData.participants.forEach((participantId, participantData) -> {
                    if (participantData.submissions.contains(submissionId)) {
                        ModFestLog.error("WTF Error: Somehow participant " + participantId + " has submission " + submissionId + " even though it has no members.");
                    }
                });
            });
            platformData.submissions.remove(submissionId);
            ModFestLog.debug("[DataManager] Removed submission '%s' entirely because it does not have any members", submissionId);
        }

        StorageManager.SUBMISSIONS.save();
        StorageManager.EVENTS.save();
    }

    public static Map<String, EventData> getEvents() {
        return platformData.events;
    }

    public static List<EventData> getEventList() {
        return new ArrayList<>(platformData.events.values());
    }

    public static boolean hasActiveEvent() {
        return configData.activeEvent == null || configData.activeEvent.isEmpty();
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
        unregister(member.getId().asString(), eventId);
        return member.removeRole(Snowflake.of(platformData.events.get(configData.activeEvent).participantRoleId));
    }

    public static void unregister(String id, String eventId) {
        platformData.events.get(eventId).participants.remove(id);
        StorageManager.EVENTS.save();
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

    public static void setSubmissions(boolean open) {
        getActiveEvent().submissionsOpen = open;
        StorageManager.EVENTS.save();
    }

    public static boolean areSubmissionsOpen() {
        return getActiveEvent().submissionsOpen;
    }

    public static void setLogInfoChannel(Snowflake channelId) {
        configData.logInfoChannelId = channelId.asString();
        StorageManager.CONFIG.save();
    }

    public static void setLogDebugChannel(Snowflake channelId) {
        configData.logDebugChannelId = channelId.asString();
        StorageManager.CONFIG.save();
    }

    public static void setLogLifecycleChannel(Snowflake channelId) {
        configData.logLifecycleChannelId = channelId.asString();
        StorageManager.CONFIG.save();
    }

    public static void setLogErrorChannel(Snowflake channelId) {
        configData.logErrorChannelId = channelId.asString();
        StorageManager.CONFIG.save();
    }

    public static void setLogAllChannel(Snowflake channelId) {
        configData.logAllChannelId = channelId.asString();
        StorageManager.CONFIG.save();
    }
}