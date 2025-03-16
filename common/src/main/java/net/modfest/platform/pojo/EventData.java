package net.modfest.platform.pojo;

import lombok.With;
import org.jspecify.annotations.NonNull;

import java.time.Instant;
import java.util.List;

@With
public record EventData(@NonNull String id,
                        @NonNull String name,
                        @NonNull String subtitle,
                        @NonNull Phase phase,
                        @NonNull List<DateRange> dates,
                        @NonNull Images images,
                        @NonNull Colors colors,
                        @NonNull DiscordRoles discordRoles,
                        @NonNull String mod_loader,
                        @NonNull String minecraft_version,
                        @NonNull String modpack) implements Data {
    public enum Type {
        MODFEST,
        BLANKETCON
    }

    public enum Phase {
        PLANNING(true, false, false),
        MODDING(true, true, true),
        BUILDING(false, false, true),
        SHOWCASE(false, false, false),
        COMPLETE(false, false, false);

        private final boolean registrations;
        private final boolean submissions;
        private final boolean updates;

        Phase(boolean registrations, boolean submissions, boolean updates) {
            this.registrations = registrations;
            this.submissions = submissions;
            this.updates = updates;
        }

        public boolean canRegister() {
            return registrations;
        }

        public boolean canSubmit() {
            return submissions;
        }

        public boolean canUpdateSubmission() {
            return updates;
        }
    }

    public record Images(String full, String transparent, String wordmark, String background) {
    }

    public record Colors(String primary, String secondary) {
    }

    public record DiscordRoles(String participant, String award) {
    }

    public record DateRange(String name, String description, Phase phase, Instant start, Instant end) {
    }
}
