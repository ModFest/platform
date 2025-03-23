package net.modfest.platform.repository;

import com.google.gson.annotations.SerializedName;
import net.modfest.platform.git.ManagedDirectory;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.HashMap;

@Repository
public class EventTokenRepository extends AbstractSingleJsonStorage<EventTokenRepository.EventTokenData> {
	// The @Qualifier("datadir") ensures that spring will give us the object marked as "datadir"
	protected EventTokenRepository(@Qualifier("datadir") ManagedDirectory datadir) {
		super(datadir.getSubFile("eventTokens.json"), EventTokenData.class);
	}

	@Override
	protected @NonNull EventTokenData createDefault() {
		return new EventTokenData(new HashMap<>());
	}

	public void deleteToken(@NonNull String eventId) {
		dataLock.writeLock().lock();
		try {
			var newMap = new HashMap<>(this.get().mcServerTokens());
			newMap.remove(eventId);
			this.save(new EventTokenData(newMap));
		} finally {
			dataLock.writeLock().unlock();
		}
	}

	public void setToken(@NonNull String eventId, @NonNull String token) {
		dataLock.writeLock().lock();
		try {
			var newMap = new HashMap<>(this.get().mcServerTokens());
			newMap.put(eventId, token);
			this.save(new EventTokenData(newMap));
		} finally {
			dataLock.writeLock().unlock();
		}
	}

	public @Nullable String getToken(@NonNull String eventId) {
		return this.get().mcServerTokens().get(eventId);
	}

	public record EventTokenData(@SerializedName("mc_server_tokens") @NonNull HashMap<@NonNull String, @NonNull String> mcServerTokens) {

	}
}
