package net.modfest.platform.security.token;

import org.apache.shiro.authc.AuthenticationToken;
import org.jspecify.annotations.Nullable;

public record EventToken(String token, @Nullable String targetUser) implements AuthenticationToken {
	@Override
	public Object getPrincipal() {
		return targetUser();
	}

	@Override
	public Object getCredentials() {
		return token();
	}

	@Override
	public String toString() {
		return "EventToken[token=***,targetUser="+targetUser+"]";
	}
}
