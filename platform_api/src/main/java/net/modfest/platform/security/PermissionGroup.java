package net.modfest.platform.security;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public enum PermissionGroup {
	UNPRIVILEGED_USERS(null, Set.of(
		Permissions.Event.SUBMIT
	)),
	TEAM_MEMBERS(UNPRIVILEGED_USERS, Set.of(
		Permissions.Meta.RELOAD,
		Permissions.Users.LIST_ALL,
		Permissions.Users.VIEW_MINECRAFT,
		Permissions.Users.VIEW_DISCORD,
		Permissions.Users.EDIT_OTHERS,
		Permissions.Users.FORCE_EDIT,
		Permissions.Event.BYPASS_REGISTRATIONS,
		Permissions.Event.REGISTER_OTHERS,
		Permissions.Event.SUBMIT_BYPASS,
		Permissions.Event.SUBMIT_OTHER,
		Permissions.Event.EDIT_SCHEDULE,
		Permissions.Event.EDIT_OTHER_SUBMISSION,
		Permissions.Event.EDIT_PHASE_BYPASS,
		Permissions.Event.MANAGE_TOKENS
	)),
	/**
	 * Note: BotFest usually performs actions on behalf of a different user.
	 * This is for actions that BotFest itself performs
	 */
	BOTFEST(null, Set.of(
		// BotFest needs to create users. It cannot perform actions on behalf of a user
		// if it doesn't exist yet.
		Permissions.Users.CREATE,
		// BotFest is able to subscribe to any changes in user data, and
		// also list all users. This is to compute the set of roles it
		// needs to assign to users
		Permissions.Users.LIST_ALL,
		// BotFest needs to look up people by their discord id, and view
		// people their discord ids
		Permissions.Users.VIEW_DISCORD
	)),
	/**
	 * Permissions given when the minecraft server logs in
	 */
	EVENT_MC_SERVER(null, Set.of(
		// The minecraft server needs to list all users in order to find anyone who
		// needs to be on the whitelist
		Permissions.Users.LIST_ALL,
		// The minecraft server needs to know the minecraft accounts associated with
		// modfest accounts in order to whitelist them
		Permissions.Users.VIEW_MINECRAFT
	));

	public final @Nullable PermissionGroup parent;
	public final Set<String> permissions;

	PermissionGroup(@Nullable PermissionGroup parent, Set<String> permissions) {
		this.parent = parent;
		this.permissions = new HashSet<>();
		this.permissions.addAll(permissions);
		if (this.parent != null) {
			this.permissions.addAll(this.parent.permissions);
		}
	}
}
