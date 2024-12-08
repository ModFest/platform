package net.modfest.platform.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

// This will get spring to import `platform.*` properties from the application.properties
// These can also be overwritten using `PLATFORM_*` environment variables
@ConfigurationProperties(prefix = "platform")
@Getter
@AllArgsConstructor
public class PlatformConfig {
	private Path datadir;
	private Path filedir;
}
