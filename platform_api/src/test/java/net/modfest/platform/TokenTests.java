package net.modfest.platform;

import net.modfest.platform.security.token.BotFestToken;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

public class TokenTests {
	@ParameterizedTest
	@MethodSource("testTokens")
	public void dontLeakSecretsInToString(Object testToken) {
		// Assert that the toString method doesn't leak any secrets
		Assertions.assertThat(testToken.toString())
			.doesNotContain("xXx_secret_xXx");
	}

	private static List<Object> testTokens() {
		return List.of(
			new BotFestToken("xXx_secret_xXx", "just some random user")
		);
	}
}
