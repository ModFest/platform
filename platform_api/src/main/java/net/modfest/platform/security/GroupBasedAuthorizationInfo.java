package net.modfest.platform.security;

import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record GroupBasedAuthorizationInfo(PermissionGroup group) implements AuthorizationInfo {
	@Override
	public Collection<String> getRoles() {
		// The closest thing we have to "roles" are a list of permission groups
		// We'll return the group as well as its parents
		// We shouldn't be using role-based permissions, instead we should use string permissions
		var roles = new ArrayList<String>();
		var currentGroup = group;

		do {
			roles.add(currentGroup.name());
			currentGroup = group.parent;
		} while (currentGroup != null);

		return roles;
	}

	@Override
	public Collection<String> getStringPermissions() {
		return group().permissions;
	}

	@Override
	public Collection<Permission> getObjectPermissions() {
		return List.of();
	}
}
