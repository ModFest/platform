package net.modfest.platform.security;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import net.modfest.platform.configuration.PlatformConfig;
import net.modfest.platform.pojo.EventData;
import net.modfest.platform.pojo.UserData;
import net.modfest.platform.security.token.BotFestToken;
import net.modfest.platform.security.token.EventToken;
import net.modfest.platform.security.token.ModrinthToken;
import net.modfest.platform.service.EventTokenService;
import net.modfest.platform.service.UserService;
import nl.theepicblock.dukerinth.ModrinthApi;
import nl.theepicblock.dukerinth.ModrinthApiException;
import org.apache.shiro.authc.*;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;

public class ModFestRealm extends AuthorizingRealm {
	@Autowired
	private UserService userService;
	@Autowired
	private EventTokenService eventTokenService;
	@Autowired
	private PlatformConfig platformConfig;
	@Autowired
	private ModrinthApi modrinthApi;
	private LoadingCache<@NonNull String, String> modrinthTokenCache = Caffeine.newBuilder()
		.maximumSize(1_000)
		.expireAfterWrite(Duration.ofMinutes(30))
		.build(this::modrinthTokenToId);

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
					throw new IncorrectCredentialsException("BotFest secret is invalid");
				}
				if (Objects.equals(botFestToken.targetUser(), "@self")) {
					// BotFest is logging in as itself, and not on behalf of a different user
					return new SimpleAuthenticationInfo(BotFestIdentity.INSTANCE, botFestToken, "platform");
				}
				var data = userService.getByDiscordId(botFestToken.targetUser());
				if (data == null) {
					throw new AuthenticationException("Can't find user with id "+botFestToken.targetUser());
				}
				return new SimpleAuthenticationInfo(data, botFestToken, "platform");
			}
			case ModrinthToken modrinthToken -> {
				try {
					var modrinthId = this.modrinthTokenCache.get(modrinthToken.token());
					if (modrinthId == null) {
						throw new IncorrectCredentialsException("Token is invalid");
					}
					var festUser = userService.getByModrinthId(modrinthId);
					if (festUser == null) {
						throw new AuthenticationException("Modrinth user "+modrinthId+" is not registered in ModFest");
					}
					return new SimpleAuthenticationInfo(festUser, modrinthToken, "platform");
				} catch (ModrinthApiException e) {
					throw new AuthenticationException("Modrinth return status code "+e.httpResponse.statusCode()+", couldn't check token", e);
				} catch (Exception e) {
					throw new AuthenticationException("Error checking modrinth token", e);
				}
			}
			case EventToken eventToken -> {
				if (eventTokenService.isTokenValid(eventToken.token())) {
					return new SimpleAuthenticationInfo(
						eventTokenService.getEventFromToken(eventToken.token()),
						eventToken,
						"platform"
					);
				} else {
					throw new IncorrectCredentialsException("Invalid event token");
				}
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
				case NONE, VOLUNTEER -> PermissionGroup.UNPRIVILEGED_USERS;
				case TEAM_MEMBER -> PermissionGroup.TEAM_MEMBERS;
			};
			return new GroupBasedAuthorizationInfo(group);
		}
		if (principalCollection.oneByType(BotFestIdentity.class) != null) {
			return new GroupBasedAuthorizationInfo(PermissionGroup.BOTFEST);
		}
		if (principalCollection.oneByType(EventData.class) != null) {
			return new GroupBasedAuthorizationInfo(PermissionGroup.EVENT_MC_SERVER);
		}
		return null;
	}

	/**
	 * Looks up a modrinth id associated with a token. Will
	 * return {@code null} if the token is invalid
	 */
	private @Nullable String modrinthTokenToId(@NonNull String token) {
		System.out.println("Modrinth auth request "+token);
		try {
			var user = modrinthApi.withAuth(token).self();
			return user.id;
		} catch (ModrinthApiException e) {
			if (e.httpResponse.statusCode() == 401) {
				return null;
			} else {
				throw e;
			}
		}
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
