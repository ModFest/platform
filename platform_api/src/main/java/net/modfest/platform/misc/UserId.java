package net.modfest.platform.misc;

import java.util.concurrent.ThreadLocalRandom;

public record UserId(String internal) {
	public static UserId generateRandom() {
		// A random userid will consist out of 5 numbers
		// the first number cannot be zero, to avoid truncating numbers
		// by converting them to/from integers
		var random = ThreadLocalRandom.current(); // No, we're not using a cryptographic random. If you manage to manipulate this and get a cool id then you deserve it.

		var numLen = 5;
		var str = new StringBuilder(numLen);
		str.append((char)('1' + random.nextInt(0, 9)));
		for (int i = 1; i < numLen; i++) {
			str.append((char)('0' + random.nextInt(0, 10)));
		}

		return new UserId(str.toString());
	}
}
