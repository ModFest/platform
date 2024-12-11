package net.modfest.platform.migrations;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.modfest.platform.misc.JsonUtil;
import net.modfest.platform.misc.MfUserId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Contains ad-hoc migrations to our json format
 */
public record Migrator(JsonUtil json, Path root) {
	static final int CURRENT_VERSION = 1;
	static final Map<Integer,MigrationManager.Migration> MIGRATIONS = new HashMap<>();

	static {
		MIGRATIONS.put(1, Migrator::migrateTo1);
	}

	/**
	 * V1, changes user id's to randomly generated strings
	 */
	public void migrateTo1() throws IOException {
		// TODO migrate submission data
		var uids = new HashSet<String>();

		var usersPath = root.resolve("users");
		var userFiles = new ArrayList<Path>();
		try (var files = Files.newDirectoryStream(usersPath)) {
			for (var file : files) {
				userFiles.add(file);
			}
		}

		for (var file : userFiles) {
			var userJson = json.readJson(file, JsonObject.class);
			String uid;
			do {
				uid = MfUserId.generateRandom();
			} while (uids.contains(uid));
			uids.add(uid);
			userJson.add("id", new JsonPrimitive(uid));
			json.writeJson(file, userJson);

			var newLocation = usersPath.resolve(uid+".json");
			Files.move(file, newLocation);
		}
	}
}
