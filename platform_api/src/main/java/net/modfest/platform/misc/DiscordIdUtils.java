package net.modfest.platform.misc;

import java.util.regex.Pattern;

public class DiscordIdUtils {
	// Biggest uint64 is 18446744073709551615
	// A snowflake generated 1 second after 2015 (discord epoch) will be 4194304
	// So that gives us a bound between 7-20 characters
	private static Pattern VALID_SNOWFLAKE = Pattern.compile("[1-9][0-9]{6,19}");

	public static boolean isValidSnowflake(String input) {
		return (VALID_SNOWFLAKE.matcher(input).matches());
	}
}
