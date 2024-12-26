package net.modfest.platform.security;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public enum PermissionGroup {
	UNPRIVILEGED_USERS(null, Set.of(
	)),
	TEAM_MEMBERS(UNPRIVILEGED_USERS, Set.of(
		Permissions.Meta.RELOAD,
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
