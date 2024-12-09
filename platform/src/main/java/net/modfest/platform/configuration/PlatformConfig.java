package net.modfest.platform.configuration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

// This will get spring to import `platform.*` properties from the application.properties
// These can also be overwritten using `PLATFORM_*` environment variables
@ConfigurationProperties(prefix = "platform")
@Getter
@AllArgsConstructor
@Validated
public class PlatformConfig {
	@NonNull
	private Path datadir;
	@NonNull
	private Path logdir;
}
