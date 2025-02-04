package net.modfest.platform.service;

import net.modfest.platform.misc.EventSource;
import net.modfest.platform.misc.MfUserId;
import net.modfest.platform.misc.PlatformStandardException;
import net.modfest.platform.pojo.PlatformErrorResponse;
import net.modfest.platform.pojo.UserCreateData;
import net.modfest.platform.pojo.UserData;
import net.modfest.platform.pojo.UserRole;
import net.modfest.platform.repository.UserRepository;
import nl.theepicblock.dukerinth.ModrinthApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;

@Service
public class UserService {
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ModrinthApi modrinthApi;

	public UserData getByMfId(String modfestId) {
		return userRepository.get(modfestId);
	}

	public UserData getByDiscordId(String discordId) {
		return userRepository.getByDiscordId(discordId);
	}

	public UserData getByModrinthId(String modrinthId) {
		return userRepository.getByModrinthId(modrinthId);
	}

	public void save(UserData data) {
		userRepository.save(data);
	}

	/**
	 * This event source will emit an event whenever new user data is saved.
	 */
	public EventSource<UserData> userEventSource() {
		return userRepository.onDataUpdated;
	}

	public Collection<UserData> getAll() {
		return userRepository.getAll();
	}

	public String create(UserCreateData data) throws InvalidModrinthIdException, PlatformStandardException {
		var mrUser = modrinthApi.users().getUser(data.modrinthId());

		if (mrUser == null) throw new InvalidModrinthIdException();

		if (userRepository.getByModrinthId(mrUser.id) != null) {
			throw new PlatformStandardException(
				PlatformErrorResponse.ErrorType.ALREADY_USED,
				new PlatformErrorResponse.AlreadyExists("modrinth", mrUser.id)
			);
		}
		if (userRepository.getByDiscordId(data.discordId()) != null) {
			throw new PlatformStandardException(
				PlatformErrorResponse.ErrorType.ALREADY_USED,
				new PlatformErrorResponse.AlreadyExists("discord", data.discordId())
			);
		}

		var generatedId = generateUserId();

		userRepository.save(new UserData(
			generatedId,
			data.name(),
			data.name(),
			data.pronouns(),
			mrUser.id,
			data.discordId(),
			mrUser.bio,
			mrUser.avatarUrl,
			Set.of(),
			Set.of(),
			UserRole.NONE
		));

		return generatedId;
	}

	/**
	 * Generates a new modfest user id. Ensures that the generated id doesn't already exist.
	 * @see MfUserId#generateRandom()
	 */
	private String generateUserId() {
		while (true) {
			var id = MfUserId.generateRandom();
			if (!userRepository.contains(id)) {
				return id;
			}
		}
	}

	public static class InvalidModrinthIdException extends Exception {

	}

	public static class UserAlreadyExistsException extends Exception {
		public UserAlreadyExistsException(String message) {
			super(message);
		}
	}
}
