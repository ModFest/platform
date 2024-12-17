package net.modfest.platform.migrations;

import lombok.With;
import net.modfest.platform.git.ManagedDirectory;
import net.modfest.platform.misc.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This is the class that manages when migrations are run.
 * If you want to write a migration, you should take a look at {@link Migrator}
 */
@Service
public class MigrationManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(MigrationManager.class);

	@Autowired
	private JsonUtil jsonUtils;

	public void migrate(ManagedDirectory root) throws IOException {
		LOGGER.info("Checking migrations for {}", root);

		var info = root.withRead(this::getInfo);
		if (info.migrationInProgress) {
			throw new IllegalStateException("A migration was started on this version, but wasn't completed. Please resolve manually");
		}

		if (info.currentVersion == Migrator.CURRENT_VERSION) {
			LOGGER.info("{} is up-to-date", root);
			return;
		}
		if (info.currentVersion > Migrator.CURRENT_VERSION) {
			throw new IllegalStateException("Data has version v"+info.currentVersion+", I only understand up to v"+ Migrator.CURRENT_VERSION);
		}

		// We need to migrate!
		LOGGER.info("Running migrations from v{} to v{}", info.currentVersion, Migrator.CURRENT_VERSION);

		for (int v = info.currentVersion; v < Migrator.CURRENT_VERSION; v++) {
			int curV = v;
			root.write(p -> {
				var curinfo = getInfo(p);

				var migrator = new Migrator(jsonUtils, p);
				var migration = Migrator.MIGRATIONS.get(curV + 1);
				if (migration == null) {
					throw new IllegalStateException("No migration function to get from v"+curV+" to v"+(curV+1));
				}
				LOGGER.info("Migrating from v{} to {}", curV, curV+1);

				curinfo = curinfo.withMigrationInProgress(true);
				writeInfo(p, curinfo); // Immediately write, if the app crashes beyond this point it'll be able to tell on next boot

				// Run migration
				migration.run(migrator);

				curinfo = new MigrationInfo(curV + 1, false);
				writeInfo(p, curinfo);
			});
		}
	}

	private void writeInfo(Path root, MigrationInfo info) throws IOException {
		jsonUtils.writeJson(root.resolve("info.json"), info);
	}

	private MigrationInfo getInfo(Path root) throws IOException {
		var migrationPath = root.resolve("info.json");
		if (!Files.exists(migrationPath)) {
			// Legacy stuff, if there's no info we assume it's version zero
			return new MigrationInfo(0, false);
		} else {
			return jsonUtils.readJson(migrationPath, MigrationInfo.class);
		}
	}

	@With
	private record MigrationInfo(int currentVersion, boolean migrationInProgress) {

	}

	@FunctionalInterface
	public interface Migration {
		void run(Migrator m) throws IOException;
	}
}
