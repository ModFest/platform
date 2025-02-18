// These definitions are very prone to becoming out-of-date

export interface CurrentEventData {
    event: string;
}

export interface EventData {
    id: string;
    name: string;
    subtitle: string;
    phase: EventData$Phase;
    dates: EventData$DateRange[];
    images: EventData$Images;
    colors: EventData$Colors;
    discordRoles: EventData$DiscordRoles;
    mod_loader: string;
    minecraft_version: string;
    modpack: string;
    description: any[];
}

export interface EventData$Colors {
    primary: string;
    secondary: string;
}

export interface EventData$DateRange {
    name: string;
    description: string;
    phase: EventData$Phase;
    start: Date;
    end: Date;
}

export interface EventData$DiscordRoles {
    participant: string;
    award: string;
}

export interface EventData$Images {
    full: string;
    transparent: string;
    wordmark: string;
    background: string;
}

export interface HealthData {
    health: string;
    runningSince: Date;
}

export interface PlatformErrorResponse {
    type: PlatformErrorResponse$ErrorType;
    data: any;
}

export interface PlatformErrorResponse$AlreadyExists {
    fieldName: string;
    content: string;
}

export interface ScheduleEntryCreate {
    event: string;
    title: string;
    type: string;
    location: string;
    description: string;
    authors: string[];
    start: Date;
    end: Date;
}

export interface ScheduleEntryData {
    id: string;
    event: string;
    title: string;
    type: string;
    location: string;
    description: string;
    authors: string[];
    start: Date;
    end: Date;
}

export interface SubmissionData {
    id: string;
    event: string;
    name: string;
    description: string;
    authors: string[];
    platform: SubmissionData$FileData;
    images: SubmissionData$Images;
    download: string;
    source: string;
    awards: SubmissionData$Awards;
}

export interface SubmissionData$Awards {
    theme: string[];
    extra: string[];
}

export interface SubmissionData$FileData {
    inner: any;
}

export interface SubmissionData$FileData$Github {
    namespace: string;
    repo: string;
}

export interface SubmissionData$FileData$Modrinth {
    projectId: string;
    versionId: string;
}

export interface SubmissionData$Images {
    icon: string;
    screenshot: string;
}

export interface SubmitRequest {
    modrinthProject: string;
}

export interface UserCreateData {
    name: string;
    pronouns: string;
    modrinthId: string;
    discordId: string;
}

export interface UserData {
    id: string;
    slug: string;
    name: string;
    pronouns: string;
    modrinth_id: string;
    discord_id: string;
    bio: string;
    icon: string;
    badges: string[];
    registered: string[];
    role: UserRole;
}

export interface UserPatchData {
    name: string;
    pronouns: string;
    bio: string;
}

export interface Whoami {
    isAuthenticated: boolean;
    userId: string;
    name: string;
    permissions: string[];
}

export type EventData$Phase = "planning" | "modding" | "building" | "showcase" | "complete";

export type EventData$Type = "modfest" | "blanketcon";

export type PlatformErrorResponse$ErrorType = "event_no_exist" | "user_no_exist" | "already_used" | "internal";

export type UserRole = "none" | "team_member";
