package net.modfest.platform.security;

import net.modfest.platform.configuration.PlatformConfig;
import net.modfest.platform.security.auth_identity.UserAuthentication;
import net.modfest.platform.security.token.BotFestToken;
import net.modfest.platform.service.UserService;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

public class ModFestRealm extends AuthenticatingRealm {
	@Autowired
	private UserService userService;
	@Autowired
	private PlatformConfig platformConfig;

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
				return new UserAuthentication(authenticationToken, data);
			}
			default -> {
				return null;
			}
		}
	}
}
