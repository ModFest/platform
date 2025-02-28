package net.modfest.platform.configuration;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.modfest.platform.git.GitRootPath;
import net.modfest.platform.migrations.MigrationManager;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

// This will get spring to import `platform.*` properties from the application.properties
// These can also be overwritten using `PLATFORM_*` environment variables
@ConfigurationProperties(prefix = "platform")
@AllArgsConstructor
@Validated
public class PlatformConfig {
	@NonNull
	private Path datadir;
	@NonNull
	@Getter
	private Path logdir;
	@Getter
	private String botFestSecret;
	@Getter
	@Nullable
	private String imageCdnUrl;
	@Getter
	@Nullable
	private String gitRemote;

	// These are bean dependencies, not a config value
	@Autowired
	private MigrationManager manager;
	@Autowired
	private GitConfig config;

	/**
	 * The data dir is only exposed as a bean in order to prevent misuse
	 */
	@Bean(name = "datadir")
	public GitRootPath getDatadir(GitConfig config) throws IOException, URISyntaxException, GitAPIException {
		var git = new GitRootPath(this.datadir, config);
		if (gitRemote != null) {
			git.addRemote(gitRemote);
		}
		return git;
	}

	@PostConstruct
	private void init() throws IOException, URISyntaxException, GitAPIException {
		// HACK: by calling this method from this init, it ensures that migrations are ran before anything
		// can even access the platform config
		manager.migrate(this.getDatadir(config));
	}
}
