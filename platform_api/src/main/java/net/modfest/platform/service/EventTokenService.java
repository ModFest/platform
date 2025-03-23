package net.modfest.platform.service;

import net.modfest.platform.misc.PlatformStandardException;
import net.modfest.platform.pojo.EventData;
import net.modfest.platform.pojo.PlatformErrorResponse;
import net.modfest.platform.repository.EventTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

@Service
public class EventTokenService {
	private final SecureRandom random = new SecureRandom();
	private static final Base64.Encoder base64 = Base64.getEncoder();
	@Autowired
	private EventTokenRepository tokenRepository;
	@Autowired
	private EventService eventService;

	public boolean isTokenValid(String token) {
		var event = getEventFromToken(token);
		if (event == null) return false;
		return Objects.equals(tokenRepository.getToken(event.id()), token);
	}

	public EventData getEventFromToken(String token) {
		var split = token.split("_");
		if (split.length < 2) return null;
		var eventPart = String.join("_", Arrays.copyOfRange(split, 0, split.length - 1));
		return eventService.getEventById(eventPart);
	}

	public void generateNewToken(String eventId) throws PlatformStandardException {
		if (eventService.getEventById(eventId) == null) {
			throw new PlatformStandardException(PlatformErrorResponse.ErrorType.EVENT_NO_EXIST, eventId);
		}

		var randomBytes = new byte[64];
		random.nextBytes(randomBytes);
		var tokenPart = base64.encodeToString(randomBytes);
		// Note that the base64 alphabet does not contain underscores. This allows us to always split
		// the eventId back
		var token = eventId+"_"+tokenPart;
		tokenRepository.setToken(eventId, token);
	}

	public void removeToken(String eventId) {
		tokenRepository.deleteToken(eventId);
	}

	public EventTokenRepository.EventTokenData getAllTokens() {
		return tokenRepository.get();
	}
}
