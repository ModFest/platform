package net.modfest.platform;

import net.modfest.platform.misc.UserId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

public class UserIdTest {
	@RepeatedTest(10000)
	public void testRandom() {
		var randomId = UserId.generateRandom();

		// User ids should have a length of 5
		Assertions.assertEquals(5, randomId.internal().length(), randomId + " has wrong length");
		// User ids cannot start with a zero. This is to avoid errors if you convert them to an integer and back
		Assertions.assertNotEquals('0', randomId.internal().charAt(0), randomId + " is invalid");
	}
}
