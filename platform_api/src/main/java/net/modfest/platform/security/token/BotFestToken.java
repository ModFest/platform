package net.modfest.platform.security.token;

import org.apache.shiro.authc.AuthenticationToken;

public record BotFestToken(String sharedSecret, String targetUser) implements AuthenticationToken {
	@Override
	public Object getPrincipal() {
		return targetUser();
	}

	@Override
	public Object getCredentials() {
		return sharedSecret();
	}
}
