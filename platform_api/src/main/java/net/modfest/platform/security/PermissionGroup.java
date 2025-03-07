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
		Permissions.Users.EDIT_OTHERS,
		Permissions.Users.FORCE_EDIT,
		Permissions.Event.BYPASS_REGISTRATIONS,
		Permissions.Event.REGISTER_OTHERS,
		Permissions.Event.SUBMIT_BYPASS,
		Permissions.Event.SUBMIT_OTHER,
		Permissions.Event.EDIT_SCHEDULE,
		Permissions.Event.EDIT_OTHER_SUBMISSION
	)),
	/**
	 * Note: BotFest usually performs actions on behalf of a different user.
	 * This is for actions that BotFest itself performs
	 */
	BOTFEST(null, Set.of(
		// BotFest needs to create users. It cannot perform actions on behalf of a user
		// if it doesn't exist yet.
		Permissions.Users.CREATE,
		// BotFest is able to subscribe to user data changing, to enable
		// it to give out roles
		Permissions.Users.LIST_ALL
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
