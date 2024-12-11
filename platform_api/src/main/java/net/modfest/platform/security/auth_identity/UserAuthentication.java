package net.modfest.platform.security.auth_identity;

import net.modfest.platform.pojo.UserData;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;

/**
 * A type of authentication that refers to a user within the platform
 */
public record UserAuthentication(AuthenticationToken token, UserData data) implements AuthenticationInfo {
	@Override
	public PrincipalCollection getPrincipals() {
		return new SimplePrincipalCollection(token.getPrincipal(), "platform");
	}

	@Override
	public Object getCredentials() {
		return token.getCredentials();
	}
}
