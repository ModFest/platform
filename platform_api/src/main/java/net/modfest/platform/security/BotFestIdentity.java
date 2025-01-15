package net.modfest.platform.security;

/**
 * In {@link ModFestRealm} we need to provide an object which marks who is logged in.
 * This is the object which is used when BotFest is logged in. (Note that BotFest will
 * usually log in as the discord user they represent, and not as itself)
 */
public class BotFestIdentity {
	public static final BotFestIdentity INSTANCE = new BotFestIdentity();

	private BotFestIdentity() {
	}
}
