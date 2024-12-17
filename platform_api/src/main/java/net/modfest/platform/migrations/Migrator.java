package net.modfest.platform.migrations;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.modfest.platform.misc.JsonUtil;
import net.modfest.platform.misc.MfUserId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Contains ad-hoc migrations to our json format
 */
public record Migrator(JsonUtil json, Path root) {
	static final int CURRENT_VERSION = 2;
	static final Map<Integer,MigrationManager.Migration> MIGRATIONS = new HashMap<>();

	static {
		MIGRATIONS.put(1, Migrator::migrateTo1);
		MIGRATIONS.put(2, Migrator::migrateTo2);
	}

	/**
	 * V1
	 * changes user id's to randomly generated strings
	 */
	public void migrateTo1() throws IOException {
		// TODO migrate submission data
		var uids = new HashSet<String>();

		var usersPath = root.resolve("users");

		MigratorUtils.executeForAllFiles(usersPath, path -> {
			var userJson = json.readJson(path, JsonObject.class);
			String uid;
			do {
				uid = MfUserId.generateRandom();
			} while (uids.contains(uid));
			uids.add(uid);
			userJson.add("id", new JsonPrimitive(uid));
			json.writeJson(path, userJson);

			var newLocation = usersPath.resolve(uid+".json");
			Files.move(path, newLocation);
		});
	}

	/**
	 * V2
	 * The "role" field for users is no longer nullable.
	 * All users now have a role. This role may be set to "NONE", but it
	 * can't be missing/null
	 */
	public void migrateTo2() throws IOException {
		var usersPath = root.resolve("users");

		MigratorUtils.executeForAllFiles(usersPath, path -> {
			var userJson = json.readJson(path, JsonObject.class);
			var role = userJson.get("role");
			if (role == null || role.isJsonNull()) {
				// Default users without roles to "none"
				userJson.add("role", new JsonPrimitive("none"));
				// Write the new json
				json.writeJson(path, userJson);
			}
  		});
	}
}
