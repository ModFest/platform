package net.modfest.platform;

import net.modfest.platform.configuration.GsonConfig;
import net.modfest.platform.misc.JsonUtil;
import net.modfest.platform.pojo.UserData;
import net.modfest.platform.pojo.UserRole;
import net.modfest.platform.repository.ConstraintViolationException;
import net.modfest.platform.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringJUnitConfig
@ContextConfiguration(classes = {JsonUtil.class, GsonConfig.class})
public class UserDbTests {
	@Autowired
	private JsonUtil json;

	public static final UserData TEST_USER = new UserData(
		"31986",
		"hatsune",
		"Hatsune Miku",
		"leek/leek",
		"lKddXfxb",
		"175928847299117063",
		"Creator of Minecraft - xoxo",
		"https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/GOODSMILE_Racing_Komatti-Mirai_EV_TT_Zero.jpg/1920px-GOODSMILE_Racing_Komatti-Mirai_EV_TT_Zero.jpg",
		Set.of(),
		Set.of(),
		Set.of(),
		UserRole.TEAM_MEMBER
	);

	@Test
	public void insertNormal(@TempDir Path dir) throws IOException {
		var testDb = new UserRepository(json, Utils.getTestDir(dir));
		testDb.readFromFilesystem(); // Initialize

		testDb.save(TEST_USER);

		assertEquals(TEST_USER, testDb.get(TEST_USER.id()));
		Assertions.assertThat(dir).isDirectoryRecursivelyContaining(path -> path.toString().endsWith(".json"));
	}


	// CONSTRAINT TESTS
	// These test if the database constraints get upheld correctly
	// these are just safety measures, but it'd be nice to ensure that they work

	@Test
	public void insertInvalidModrinth(@TempDir Path dir) throws IOException {
		var testDb = new UserRepository(json, Utils.getTestDir(dir));
		testDb.readFromFilesystem(); // Initialize

		assertThrows(
			ConstraintViolationException.class,
			() -> testDb.save(TEST_USER.withModrinthId("this is not a modrinth id"))
		);
	}

	@Test
	public void insertInvalidDiscord(@TempDir Path dir) throws IOException {
		var testDb = new UserRepository(json, Utils.getTestDir(dir));
		testDb.readFromFilesystem(); // Initialize

		assertThrows(
			ConstraintViolationException.class,
			() -> testDb.save(TEST_USER.withDiscordId("this is not a snowflake"))
		);
	}

	/**
	 * Test that two users can't have the same modrinth id in the database
	 */
	@Test
	public void insertDoubleModrinth(@TempDir Path dir) throws IOException {
		var testDb = new UserRepository(json, Utils.getTestDir(dir));
		testDb.readFromFilesystem(); // Initialize

		testDb.save(TEST_USER);
		// We need to change the id (otherwise we wouldn't be inserting a new user,
		// just changing the old one) and we need to change the discord id (because
		// we want to test only the modrinth duplicate detection)
		var newUser = TEST_USER.withId("36873").withDiscordId("175928847299117064");
		var e = assertThrows(
			ConstraintViolationException.class,
			() -> testDb.save(newUser)
		);

		// The error message should be helpful
		Assertions.assertThatCharSequence(e.getMessage()).containsIgnoringCase("modrinth");
	}

	/**
	 * Test that two users can't have the same discord id in the database
	 */
	@Test
	public void insertDoubleDiscord(@TempDir Path dir) throws IOException {
		var testDb = new UserRepository(json, Utils.getTestDir(dir));
		testDb.readFromFilesystem(); // Initialize

		testDb.save(TEST_USER);
		// We need to change the id (otherwise we wouldn't be inserting a new user,
		// just changing the old one) and we need to change the modrinth id (because
		// we want to test only the discord duplicate detection)
		var newUser = TEST_USER.withId("36873").withModrinthId("aYxYcZb8");
		var e = assertThrows(
			ConstraintViolationException.class,
			() -> testDb.save(newUser)
		);

		// The error message should be helpful
		Assertions.assertThatCharSequence(e.getMessage()).containsIgnoringCase("discord");
	}
}
