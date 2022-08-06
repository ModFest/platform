package fr.minemobs.modrinthjavalib.project.version;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class Version {

	public boolean featured;

	@SerializedName("version_type")
	public VersionType versionType;

	public String changelog;

	@SerializedName("version_number")
	public String versionNumber;

	@SerializedName("changelog_url")
	public String changelogUrl;

	public List<String> dependencies;
	public List<String> loaders;

	@SerializedName("project_id")
	public String projectId;

	@SerializedName("date_published")
	public String datePublished;

	public int downloads;
	public String name;
	public List<FilesItem> files;
	public String id;

	@SerializedName("game_versions")
	public List<String> gameVersions;

	@SerializedName("author_id")
	public String authorId;

	public boolean isFeatured() {
		return featured;
	}

	public VersionType getVersionType() {
		return versionType;
	}

	public String getChangelog() {
		return changelog;
	}

	/**
	 * @return The version number. Ideally will follow semantic versioning
	 */
	public String getVersionNumber() {
		return versionNumber;
	}

	/**
	 * @deprecated Use {@link #getChangelog()} instead since changelogs are directly hosted on Modrinth.
	 */
	@Deprecated
	public String getChangelogUrl() {
		return changelogUrl;
	}

	/**
	 * @return A list of specific versions of projects that this version depends on
	 */
	public List<String> getDependencies() {
		return dependencies;
	}

	public List<String> getLoaders() {
		return loaders;
	}

	public String getProjectId() {
		return projectId;
	}

	public String getDatePublished() {
		return datePublished;
	}

	public int getDownloads() {
		return downloads;
	}

	public String getName() {
		return name;
	}

	public List<FilesItem> getFiles() {
		return files;
	}

	public String getId() {
		return id;
	}

	public List<String> getGameVersions() {
		return gameVersions;
	}

	public String getAuthorId() {
		return authorId;
	}
}