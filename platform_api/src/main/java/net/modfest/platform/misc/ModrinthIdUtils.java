package net.modfest.platform.misc;

import java.util.regex.Pattern;

public class ModrinthIdUtils {
	// Modrinth's implementation of base62 will not add leading zeros.
	// It'd be the equivalent of writing 87 as 000087 in decimal
	private static Pattern VALID_BASE62_CHARS = Pattern.compile("[1-9a-zA-Z][0-9a-zA-Z]+");
	private static String MAX_ID = "LygHa16AHYF"; // This is the equivalent of 2^64, written in base62

	/**
	 * Checks if a string is a valid modrinth user id. Does not accept slugs.
	 */
	public static boolean isValidModrinthId(String id) {
		if (id.length() < 8) {
			// User ids have a minimum length of 8 characters
			// https://github.com/modrinth/code/blob/0d7934e3b8e38ca426c61e9f2f504c7530f58b9f/apps/labrinth/src/database/models/ids.rs#L137
			return false;
		}

		// Tries to catch things that clearly aren't generated by:
		// https://github.com/modrinth/code/blob/0d7934e3b8e38ca426c61e9f2f504c7530f58b9f/apps/labrinth/src/models/v3/ids.rs#L195
		if (!VALID_BASE62_CHARS.matcher(id).matches()) {
			return false;
		}

		// Due to the pattern, we now know that the string does not contain any wierd
		// unicode that can mess up .length or .charAt calls

		// If you look at https://github.com/modrinth/code/blob/0d7934e3b8e38ca426c61e9f2f504c7530f58b9f/apps/labrinth/src/models/v3/ids.rs#L43
		// and https://github.com/modrinth/code/blob/0d7934e3b8e38ca426c61e9f2f504c7530f58b9f/apps/labrinth/src/database/models/ids.rs#L19
		// modrinth will never generate anything that isn't exactly of length 8.
		// But modrinth does have the code to generate longer ids (they do so for notifications)
		// and I don't think they really put any strong guarantees on there being a length of 8
		// To have at least some kind of future proofing, we're only going to check
		// for anything that still fits in a 64 bit integer
		// See also: https://discord.com/channels/734077874708938864/734077874708938867/1041922754242416661 (Modrinth discord)

		if (id.length() > MAX_ID.length()) {
			return false;
		}

		if (id.length() == MAX_ID.length()) {
			for (int i = 0; i < id.length(); i++) {
				var c = id.charAt(i);
				var max = MAX_ID.charAt(i);
				// Base62 ordering matches ascii
				if (c > max) {
					return false; // This number is larger than 2^64, not a modrinth id
				}
				if (c < max) {
					break; // No need to check the rest of the numbers
				}
			}
		}

		// Looks all good!
		return true;
	}
}
