package net.modfest.platform.controller;

import net.modfest.platform.misc.PlatformStandardException;
import net.modfest.platform.repository.EventTokenRepository;
import net.modfest.platform.security.Permissions;
import net.modfest.platform.service.EventTokenService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class EventTokenController {
	@Autowired
	private EventTokenService service;

	@GetMapping("/event_tokens/")
	@RequiresPermissions(Permissions.Event.MANAGE_TOKENS)
	public EventTokenRepository.EventTokenData getAllTokens() {
		return service.getAllTokens();
	}

	@DeleteMapping("/event_tokens/{eventId}")
	@RequiresPermissions(Permissions.Event.MANAGE_TOKENS)
	public void deprecateEventToken(@PathVariable String eventId) {
		service.removeToken(eventId);
	}

	@PostMapping("/event_tokens/{eventId}/regenerate")
	@RequiresPermissions(Permissions.Event.MANAGE_TOKENS)
	public void regenerateEventToken(@PathVariable String eventId) throws PlatformStandardException {
		service.generateNewToken(eventId);
	}
}
