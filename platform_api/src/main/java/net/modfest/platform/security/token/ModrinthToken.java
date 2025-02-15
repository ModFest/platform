package net.modfest.platform.security.token;

import org.apache.shiro.authc.AuthenticationToken;

public record ModrinthToken(String token) implements AuthenticationToken {
	@Override
	public Object getPrincipal() {
		return null;
	}

	@Override
	public Object getCredentials() {
		return token();
	}

	@Override
	public String toString() {
		return "ModrinthToken[token=***]";
	}
}
