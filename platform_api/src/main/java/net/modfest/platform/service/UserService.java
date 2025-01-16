package net.modfest.platform.service;

import net.modfest.platform.misc.MfUserId;
import net.modfest.platform.pojo.UserCreateData;
import net.modfest.platform.pojo.UserData;
import net.modfest.platform.pojo.UserRole;
import net.modfest.platform.repository.UserRepository;
import nl.theepicblock.dukerinth.ModrinthApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

	public void save(UserData data) throws IOException {
		userRepository.save(data);
	}

	public UserData getByDiscordId(String discordId) {
		return userRepository.getByDiscordId(discordId);
	}

	public Collection<UserData> getAll() {
		return userRepository.getAll();
	}

	public void create(UserCreateData data) throws IOException, InvalidModrinthIdException, UserAlreadyExistsException {
		var mrUser = modrinthApi.users().getUser(data.modrinthId());

		if (mrUser == null) throw new InvalidModrinthIdException();

		if (userRepository.getByModrinthId(mrUser.id) != null) {
			throw new UserAlreadyExistsException("A user with modrinth id "+mrUser.id+" already exists!");
		}
		if (userRepository.getByDiscordId(data.discordId()) != null) {
			throw new UserAlreadyExistsException("A user with discord id "+data.discordId()+" already exists!");
		}

		userRepository.save(new UserData(
			generateUserId(),
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
