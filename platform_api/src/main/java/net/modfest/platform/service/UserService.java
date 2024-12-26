package net.modfest.platform.service;

import net.modfest.platform.pojo.UserData;
import net.modfest.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;

@Service
public class UserService {
	@Autowired
	private UserRepository userRepository;

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
}
