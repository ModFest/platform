package fr.minemobs.modrinthjavalib.project;

import fr.minemobs.modrinthjavalib.project.version.Version;

public class Dependencies {

    public final Version[] versions;
    public final Project[] projects;

    public Dependencies(Version[] versions, Project[] projects) {
        this.versions = versions;
        this.projects = projects;
    }

    public Version[] getVersions() {
        return versions;
    }

    public Project[] getProjects() {
        return projects;
    }
}
