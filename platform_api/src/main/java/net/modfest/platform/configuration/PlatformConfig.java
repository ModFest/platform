package net.modfest.platform.configuration;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.modfest.platform.migrations.MigrationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
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
	private String botFestSecret;

	@Autowired
	private MigrationManager manager;

	@PostConstruct
	private void init() throws IOException {
		// HACK: by calling this method from this init, it ensures that migrations are ran before anything
		// can even access the platform config
		manager.migrate(this.getDatadir());
	}
}
