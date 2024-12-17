package net.modfest.platform.security;

import net.modfest.platform.configuration.PlatformConfig;
import net.modfest.platform.pojo.UserData;
import net.modfest.platform.security.token.BotFestToken;
import net.modfest.platform.service.UserService;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Objects;

public class ModFestRealm extends AuthorizingRealm {
	@Autowired
	private UserService userService;
	@Autowired
	private PlatformConfig platformConfig;

	public ModFestRealm() {
		this.setCredentialsMatcher(new AllowAllCredentialsMatcher());
	}

	@Override
	public String getName() {
		return "platform";
	}

	@Override
	public boolean supports(AuthenticationToken token) {
		return true;
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
		switch (authenticationToken) {
			case BotFestToken botFestToken -> {
				if (!Objects.equals(botFestToken.sharedSecret(), platformConfig.getBotFestSecret())) {
					throw new AuthenticationException("BotFest secret is invalid");
				}
				var data = userService.getByDiscordId(botFestToken.targetUser());
				if (data == null) {
					throw new AuthenticationException("Can't find user with id "+botFestToken.targetUser());
				}
				return new SimpleAuthenticationInfo(data, botFestToken, "platform");
			}
			default -> {
				return null;
			}
		}
	}

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
		// If the user has modfest user data attached in their authenticated info,
		// we'll use that to determine which role they have!
		var user = principalCollection.oneByType(UserData.class);
		if (user != null) {
			var userRole = user.role();
			var group = switch (userRole) {
				case null -> PermissionGroup.UNPRIVILEGED_USERS;
				case NONE -> PermissionGroup.UNPRIVILEGED_USERS;
				case TEAM_MEMBER -> PermissionGroup.TEAM_MEMBERS;
			};
			return new GroupBasedAuthorizationInfo(group);
		}
		return null;
	}

	/**
	 * @deprecated This is public only for debugging purposes. Use Shiro's api's to check for permissions
	 */
	@Deprecated
	public Collection<String> getPermissions(PrincipalCollection principalCollection) {
		var info = this.getAuthorizationInfo(principalCollection);
		return info == null ? null : info.getStringPermissions();
	}
}
