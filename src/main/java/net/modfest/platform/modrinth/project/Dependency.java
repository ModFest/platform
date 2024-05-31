package net.modfest.platform.modrinth.project;

import com.google.gson.annotations.SerializedName;

public class Dependency {
    @SerializedName("version_id")
    public String versionId;
    @SerializedName("project_id")
    public String projectId;
    @SerializedName("dependency_type")
    public DependencyType dependencyType;
}
