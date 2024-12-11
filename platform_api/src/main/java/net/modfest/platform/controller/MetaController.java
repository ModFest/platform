package net.modfest.platform.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import net.modfest.platform.pojo.HealthData;
import net.modfest.platform.pojo.UserData;
import net.modfest.platform.service.MetaService;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

/**
 * Various meta endpoints that interact with platform itself, instead of its data
 */
@RestController
public class MetaController {
	@Autowired
	private MetaService metaService;
	private Date startupTime;

	@PostConstruct
	public void init() {
		// Record when the controller is initializer, this should give us an approximation for
		// how long the server has been up
		this.startupTime = Date.from(Instant.now());
	}

	@Operation(summary = "Useful endpoint to see if the platform is still running")
	@GetMapping("/health")
	public HealthData getHealth() {
		return new HealthData(
			"\uD83D\uDC4D", // thumbs-up emoji
			this.startupTime
		);
	}

	@Operation(summary = "Causes the platform to reload its caches from disk",
		description = "Invalidates the in-memory caches. Will return the amount of stores that were invalidated " +
			"(as an indication that it's doing something)")
	@PostMapping("/meta/reload")
	public int reload() throws IOException {
		return metaService.reloadFromDisk();
	}


	@Operation(summary = "Get information about the currently logged-in user",
			   description = "This route is intended to allow you to debug if you're logged in")
	@GetMapping("/meta/me")
	public MeInfo aboutLoggedInUser() {
		var subject = SecurityUtils.getSubject();
		var principal = SecurityUtils.getSubject().getPrincipal();

		String userId = null;
		String name = null;

		switch (principal) {
			case UserData user -> {
				userId = user.id();
				name = user.name();
			}
			default -> {}
		}

		return new MeInfo(
			subject.isAuthenticated(),
			userId,
			name
		);
	}

	private record MeInfo(boolean isAuthenticated, @Nullable String userId, @Nullable String name) {}
}
