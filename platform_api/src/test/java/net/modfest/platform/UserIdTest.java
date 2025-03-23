package net.modfest.platform;

import net.modfest.platform.misc.MfUserId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UserIdTest {
	@Test
	public void testRandom() {
		for (int i = 0; i < 2000; i++) {
			var randomId = MfUserId.generateRandom();

			// User ids should have a length of 5
			Assertions.assertEquals(5, randomId.length(), randomId + " has wrong length");
			// User ids cannot start with a zero. This is to avoid errors if you convert them to an integer and back
			Assertions.assertNotEquals('0', randomId.charAt(0), randomId + " is invalid");
		}
	}
}
