package net.modfest.platform;

import net.modfest.platform.misc.MfUserId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

public class UserIdTest {
	@RepeatedTest(10000)
	public void testRandom() {
		var randomId = MfUserId.generateRandom();

		// User ids should have a length of 5
		Assertions.assertEquals(5, randomId.length(), randomId + " has wrong length");
		// User ids cannot start with a zero. This is to avoid errors if you convert them to an integer and back
		Assertions.assertNotEquals('0', randomId.charAt(0), randomId + " is invalid");
	}
}
