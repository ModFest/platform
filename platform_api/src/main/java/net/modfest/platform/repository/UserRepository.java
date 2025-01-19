package net.modfest.platform.repository;

import net.modfest.platform.git.ManagedDirectory;
import net.modfest.platform.misc.DiscordIdUtils;
import net.modfest.platform.misc.ModrinthIdUtils;
import net.modfest.platform.pojo.UserData;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.Objects;

@Repository
public class UserRepository extends AbstractJsonRepository<UserData> {
	public UserRepository(@Qualifier("datadir") ManagedDirectory datadir) {
		super(datadir.getSubDirectory("users"), "user", UserData.class);
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

	@Override
	protected void validateEdit(UserData previous, UserData current) throws ConstraintViolationException {
		// USER DATABASE CONSTRAINTS
		// modrinth id must be a valid id (no slugs!)
		// discord id must be a valid snowflake
		// no duplicate modrinth ids
		// no duplicate discord ids

		if (previous == null || !Objects.equals(current.modrinthId(), previous.modrinthId())) {
			// Mr id changed
			if (!ModrinthIdUtils.isValidModrinthId(current.modrinthId())) {
				// No invalid ids allowed in the db
				// Also no slugs allowed (since then we wouldn't be able to do proper checking
				// for duplicate ids. Also slugs can change, and that's undesirable)
				throw new ConstraintViolationException("Invalid modrinth id "+current.modrinthId());
			}
			var existing = this.getByModrinthId(current.modrinthId());
			if (existing != null) {
				// There's already an object which owns this modrinth id
				throw new ConstraintViolationException("Modrinth id '"+current.modrinthId()+"' must be unique, but it's already in the database");
			}
		}

		if (previous == null || !Objects.equals(current.discordId(), previous.discordId())) {
			if (!DiscordIdUtils.isValidSnowflake(current.modrinthId())) {
				throw new ConstraintViolationException("Invalid discord id "+current.discordId());
			}
			var existing = this.getByDiscordId(current.discordId());
			if (existing != null) {
				throw new ConstraintViolationException("Discord id '"+current.discordId()+"' must be unique, but it's already in the database");
			}
		}
	}
}
