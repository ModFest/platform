package net.modfest.platform.configuration;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// This will get spring to import `git.*` properties from the application.properties
// These can also be overwritten using `GIT_*` environment variables
@ConfigurationProperties(prefix = "git")
@AllArgsConstructor
@Validated
@Getter
public class GitConfig {
	@NonNull
	private String user;
	@NonNull
	@Email
	private String email;
	@Nullable
	private String authuser;
	@Nullable
	private String authpassword;
}
