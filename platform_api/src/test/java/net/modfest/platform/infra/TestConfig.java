package net.modfest.platform.infra;

import jakarta.annotation.PreDestroy;
import net.modfest.platform.configuration.GitConfig;
import net.modfest.platform.git.GitRootPath;
import net.modfest.platform.migrations.MigrationManager;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

@TestConfiguration
public class TestConfig {
	private Path tempDir;

	@Profile("test")
	@Bean(name = "datadir")
	public GitRootPath datadirForTest(GitConfig config, MigrationManager manager) throws IOException, URISyntaxException, GitAPIException {
		if (this.tempDir == null) {
			this.tempDir = Files.createTempDirectory("modfest-platform-test");
		}

		var git = new GitRootPath(this.tempDir, config);
		manager.migrate(git);
		return git;
	}

	@PreDestroy
	public void onBeanDestroy() throws IOException {
		if (this.tempDir != null) {
			FileUtils.deleteDirectory(this.tempDir.toFile());
		}
	}
}
