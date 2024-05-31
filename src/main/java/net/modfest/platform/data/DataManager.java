package net.modfest.platform.data;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import net.modfest.platform.log.ModFestLog;
import net.modfest.platform.modrinth.Modrinth;
import net.modfest.platform.modrinth.project.Project;
import net.modfest.platform.modrinth.project.version.FilesItem;
import net.modfest.platform.modrinth.user.User;
import net.modfest.platform.pojo.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        StorageManager.saveUser(data);
    }

    public static void addSubmission(String userId, SubmissionData newSubmission, String eventId) {
        ModFestLog.debug("[DataManager] Adding new submission: %s", newSubmission.toString());

        platformData.submissions.putIfAbsent(eventId, new HashMap<>());
        SubmissionData submission = platformData.submissions.get(eventId)
                .compute(newSubmission.id(), (id, existing) -> {
                    if (existing == null) {
                        return newSubmission;
                    }

                    existing.authors().add(userId);
                    return existing;
                });
        StorageManager.saveSubmission(eventId, submission);
    }

    public static String updateSubmissionData(String submissionId) {
        try {
            SubmissionData submission = getSubmission(submissionId);
            if (submission == null) {
                throw new RuntimeException("Could not find submission with ID: " + submissionId);
            }
            switch (submission.platform()) {
                case SubmissionData.Platform.Modrinth(String projectId, String versionId) ->
                        updateSubmissionData(submission, Modrinth.getProject(projectId));
                default -> {
                    return null;
                }
            }
            throw new RuntimeException("Submisson " + submissionId + " is not a Modrinth submission.");
        } catch (Throwable e) {
            return e.getMessage();
        }
    }

    public static String setVersion(String submissionId, String versionId) {
        try {
            var submission = getSubmission(submissionId);

            switch(submission.platform()) {
                case SubmissionData.Platform.Modrinth modrinth -> {
                    String projectId = modrinth.projectId();
                    var version = Modrinth.getProject(projectId).getVersion(versionId);
                    submission.setPlatform(new SubmissionData.Platform.Modrinth(projectId, versionId));
                    submission.setDownload(version.getFiles()
                            .stream()
                            .filter(FilesItem::isPrimary)
                            .findFirst()
                            .orElseGet(() -> version.getFiles().getFirst()).url);
                }
                default -> throw new RuntimeException("Submission is not associated with a Modrinth project.");
            }
            return null;
        } catch (Throwable e) {
            return e.getMessage();
        }
    }

    public static SubmissionData getSubmissionFromModrinthId(String modrinthId) {
        return getSubmissionList().stream()
                .filter(submission -> switch (submission.platform()) {
                    case SubmissionData.Platform.Modrinth(String projectId, String versionId) ->
                            modrinthId.equals(projectId);
                    default -> false;
                })
                .findAny()
                .orElseThrow(() -> new RuntimeException("Could not find submission with Modrinth ID: " + modrinthId));
    }

    public static void updateSubmissionData(SubmissionData submission, Project modrinthProject) {
        submission.setName(modrinthProject.title);
        submission.setDescription(modrinthProject.description);
        submission.setImages(modrinthProject.getImages());
        submission.setSource(modrinthProject.sourceUrl);
        StorageManager.saveSubmission(submission.getEvent().id(), submission);
    }

    public static void removeSubmission(Snowflake discordId, String submissionId, String eventId) {
        SubmissionData submission = platformData.submissions.get(eventId).get(submissionId);
        UserData user = getUser(discordId);
        submission.authors().remove(user.id());
        ModFestLog.debug("[DataManager] Removed submission '%s' from user '%s' for event '%s'",
                submissionId,
                discordId.asString(),
                eventId);
        if (submission.authors().isEmpty()) {
            platformData.submissions.remove(submissionId);
            ModFestLog.debug("[DataManager] Removed submission '%s' entirely because it does not have any authors",
                    submissionId);
        }
        StorageManager.saveSubmission(eventId, submissionId);
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

    public static Map<String, Map<String, SubmissionData>> getSubmissions() {
        return platformData.submissions;
    }

    public static SubmissionData getSubmission(String id) {
        return getSubmissionList().stream()
                .filter(submissionData -> submissionData.id().equals(id))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Could not find submission with ID: " + id));
    }

    public static Map<String, SubmissionData> getSubmissions(String event) {
        return platformData.submissions.getOrDefault(event, new HashMap<>());
    }

    public static List<SubmissionData> getSubmissionList() {
        return platformData.submissions.values()
                .stream()
                .flatMap(submissionMap -> submissionMap.values().stream())
                .toList();
    }

    public static Map<String, SubmissionData> getSubmissionsFlat() {
        return platformData.submissions.values()
                .stream()
                .flatMap(submissionMap -> submissionMap.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static Mono<Void> register(Member discordMember, String eventId) {
        UserData user = getUser(discordMember);
        user.registered().add(eventId);
        StorageManager.saveUser(user);
        return discordMember.addRole(Snowflake.of(platformData.events.get(eventId).discordRoles().participant()))
                .and(discordMember.addRole(getUserRole()));
    }

    public static Mono<Void> unregister(Member discordMember, String eventId) {
        var user = getUser(discordMember);
        unregister(user, eventId);
        return discordMember.removeRole(Snowflake.of(platformData.events.get(eventId).discordRoles().participant()));
    }

    public static UserData getUser(Member discordMember) {
        return getUser(discordMember.getId());
    }

    public static UserData getUser(Snowflake discordId) {
        String discordIdString = discordId.asString();
        return platformData.users.values()
                .stream()
                .filter(userData -> userData.discordId().equals(discordIdString))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Could not find user with Discord ID: " + discordIdString));
    }

    public static void unregister(UserData user, String eventId) {
        user.registered().remove(eventId);
        StorageManager.saveUser(user);
    }

    public static boolean isRegistered(Snowflake discordId, String eventId) {
        return getUser(discordId).registered().contains(eventId);
    }

    public static String updateUserData(Snowflake discordId) {
        var user = getUser(discordId);
        if (user.modrinthId() == null) {
            throw new RuntimeException("User '" + user.id() + "' does not have modrinth ID, cannot update user data from Modrinth");
        }
        try {
            updateUserData(discordId, Modrinth.getUser(user.modrinthId()));
            return null;
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    public static void updateUserDisplayName(Snowflake id, String displayName) {
        var user = getUser(id);
        user.setName(displayName);
        StorageManager.saveUser(user);
    }

    public static void updateUserPronouns(Snowflake id, String pronouns) {
        var user = getUser(id);
        user.setPronouns(pronouns);
        StorageManager.saveUser(user);
    }

    public static void updateUserData(Snowflake id, User modrinthData) {
        var user = getUser(id);
        user.setIcon(modrinthData.avatarUrl);
        user.setBio(modrinthData.bio);
        StorageManager.saveUser(user);
    }

    public static void setPhase(EventData.Phase phase) {
        getActiveEvent().setPhase(phase);
        StorageManager.saveEvents();
    }

    public static boolean areSubmissionsOpen() {
        return getActiveEvent().phase().submissionsAndRegistrationsOpen();
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