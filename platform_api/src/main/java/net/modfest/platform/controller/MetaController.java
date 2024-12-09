package net.modfest.platform.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.PostConstruct;
import net.modfest.platform.pojo.HealthData;
import net.modfest.platform.service.MetaService;
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

	@Operation(summary = "Causes the platform to reload its caches from disk")
	@PostMapping("/meta/reload")
	public void reload() throws IOException {
		metaService.reloadFromDisk();
	}
}
