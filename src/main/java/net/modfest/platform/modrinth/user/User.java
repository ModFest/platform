package net.modfest.platform.modrinth.user;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.modfest.platform.modrinth.Modrinth;

import java.util.Date;

public class User {

	public Role role;

	@SerializedName("avatar_url")
	public String avatarUrl;

	public Date created;
	public String name;
	public String bio;

	@SerializedName("github_id")
	public int githubId;
	public String id;
	public String email;
	public String username;

	@Expose(deserialize = false, serialize = false)
	private transient Modrinth modrinth;

	public Role getRole() {
		return role;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	/**
	 * @return The time at which the user was created
	 */
	public Date getCreated() {
		return created;
	}

	public String getName() {
		return name;
	}

	public String getBio() {
		return bio;
	}

	public int getGithubId() {
		return githubId;
	}

	public String getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getUsername() {
		return username;
	}

	public void setModrinth(Modrinth modrinth) {
		if(this.modrinth != null || Thread.currentThread().getStackTrace()[1].getClassName().equals(Modrinth.class.getName())) return;
		this.modrinth = modrinth;
	}
}