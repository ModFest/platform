package net.modfest.platform.migrations;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.modfest.platform.misc.JsonUtil;
import net.modfest.platform.misc.MfUserId;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

/**
 * Contains ad-hoc migrations to our json format
 */
public record Migrator(JsonUtil json, Path root) {
	static final int CURRENT_VERSION = 4;
	static final Map<Integer,MigrationManager.Migration> MIGRATIONS = new HashMap<>();

	static {
		MIGRATIONS.put(1, Migrator::migrateTo1);
		MIGRATIONS.put(2, Migrator::migrateTo2);
		MIGRATIONS.put(3, Migrator::migrateTo3);
		MIGRATIONS.put(4, Migrator::migrateTo4);
	}


	/**
	 * V1
	 * Migrates from the old platform data format.
	 * Changes: submissions now explicitly store which event they're from
	 */
	public void migrateTo1() {
		var submissionPath = root.resolve("submissions");

		MigratorUtils.executeForAllFiles(submissionPath, folder -> {
			if (!Files.isDirectory(folder)) {
				return;
			}

			var eventName = folder.getFileName().toString();
			MigratorUtils.executeForAllFiles(folder, event -> {
				var eventJson = json.readJson(event, JsonObject.class);
				eventJson.add("event", new JsonPrimitive(eventName));
				json.writeJson(event, eventJson);
			});
		});
	}

	/**
	 * V2
	 * changes user id's to randomly generated strings
	 */
	public void migrateTo2() {
		var uids = new HashSet<String>();
		var old2New = new HashMap<String, String>();

		var usersPath = root.resolve("users");

		MigratorUtils.executeForAllFiles(usersPath, path -> {
			var userJson = json.readJson(path, JsonObject.class);
			var oldId = userJson.getAsJsonPrimitive("id").getAsString();
			String uid;
			do {
				uid = MfUserId.generateRandom();
			} while (uids.contains(uid));
			uids.add(uid);
			userJson.add("id", new JsonPrimitive(uid));
			json.writeJson(path, userJson);

			var newLocation = usersPath.resolve(uid+".json");
			Files.move(path, newLocation);

			old2New.put(oldId, uid);
		});

		// Edit submissions to match new ids
		var newIdentities = new HashMap<String, String>();
		var eventParticipants = new HashMap<String, HashSet<String>>();
		var submissions = root.resolve("submissions");
		MigratorUtils.executeForAllFiles(submissions, eventDir -> {
			var eventName = eventDir.getFileName().toString();
			eventParticipants.put(eventName, new HashSet<>());
			MigratorUtils.executeForAllFiles(eventDir, submissionFile -> {
				var submission = json.readJson(submissionFile, JsonObject.class);
				var authors = submission.getAsJsonArray("authors");
				var newAuthors = new JsonArray();
				for (var a : authors) {
					var author = a.getAsString();
					if (!old2New.containsKey(author) && !old2New.containsKey(author.toLowerCase(Locale.ROOT))) {
						// This is a user who isn't present in our users list!
						// we will create them a new identity
						String uid;
						do {
							uid = MfUserId.generateRandom();
						} while (uids.contains(uid));
						old2New.put(author, uid);
						newIdentities.put(author, uid);
						eventParticipants.get(eventName).add(uid);
						newAuthors.add(uid);
					} else {
						var uid = old2New.get(author);
						if (uid == null) uid = old2New.get(author.toLowerCase(Locale.ROOT));
						eventParticipants.get(eventName).add(uid);
						newAuthors.add(uid);
					}
				}
				submission.add("authors", newAuthors);
				json.writeJson(submissionFile, submission);
			});
		});

		// Create json's for the new identities
		for (var identity : newIdentities.entrySet()) {
			var id = identity.getValue();
			var name = identity.getKey();
			var path = usersPath.resolve(id+".json");

			var userJson = new JsonObject();
			userJson.addProperty("id", id);
			userJson.add("slug", JsonNull.INSTANCE);
			userJson.addProperty("name", name);
			userJson.add("pronouns", JsonNull.INSTANCE);
			userJson.add("modrinth_id", JsonNull.INSTANCE);
			userJson.add("discord_id", JsonNull.INSTANCE);
			userJson.add("icon", JsonNull.INSTANCE);
			userJson.add("badges", new JsonArray());
			var r = new JsonArray();
			for (var participation : eventParticipants.entrySet()) {
				if (participation.getValue().contains(id)) {
					r.add(participation.getKey());
				}
			}
			userJson.add("registered", r);
			userJson.addProperty("role", "none");

			json.writeJson(path, userJson);
		}
	}

	/**
	 * V3
	 * The "role" field for users is no longer nullable.
	 * All users now have a role. This role may be set to "NONE", but it
	 * can't be missing/null
	 */
	public void migrateTo3() {
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

	/**
	 * V4
	 * Changes timestamps to ISO8086
	 */
	public void migrateTo4() {
		var oldFormat = new SimpleDateFormat("MMM dd yyyy h:mm:ssa", Locale.ROOT);

		Function<String, String> toIso = str -> {
			try {
				// These idiosyncrasies sometimes occur within the old format
				str = str.replace(" ", "");
				str = str.replace(",", "");
				if (str.startsWith("Sept ")) {
					str = str.replaceFirst("Sept ", "Sep ");
				}
				if (str.endsWith(" AM")) {
					str = str.replace(" AM", "AM");
				}
				if (str.endsWith(" PM")) {
					str = str.replace(" PM", "PM");
				}
				var date = oldFormat.parse(str);
				return date.toInstant().toString();
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		};
		var eventPath = root.resolve("events");

		MigratorUtils.executeForAllFiles(eventPath, path -> {
			var eventData = json.readJson(path, JsonObject.class);
			var dates = eventData.getAsJsonArray("dates");
			for (var date : dates) {
				var o = date.getAsJsonObject();
				o.addProperty("start", toIso.apply(o.get("start").getAsString()));
				o.addProperty("end", toIso.apply(o.get("end").getAsString()));
			}
			json.writeJson(path, eventData);
		});
	}
}
