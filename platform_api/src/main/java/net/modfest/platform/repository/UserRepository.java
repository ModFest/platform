package net.modfest.platform.repository;

import net.modfest.platform.pojo.UserData;
import org.springframework.stereotype.Repository;

import java.util.Objects;

@Repository
public class UserRepository extends AbstractJsonRepository<UserData> {
	public UserRepository() {
		super("users", UserData.class);
	}

	public UserData getByDiscordId(String discordId) {
		return this.getAll()
			.stream()
			.filter(user -> Objects.equals(user.discordId(), discordId))
			.findFirst()
			.orElse(null);
	}

	public UserData getByModrinthId(String modrinthId) {
		return this.getAll()
			.stream()
			.filter(user -> Objects.equals(user.modrinthId(), modrinthId))
			.findFirst()
			.orElse(null);
	}
}
