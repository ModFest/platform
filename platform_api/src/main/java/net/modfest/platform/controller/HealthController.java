package net.modfest.platform.controller;

import jakarta.annotation.PostConstruct;
import net.modfest.platform.pojo.HealthData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Date;

@RestController
public class HealthController {
	private Date startupTime;

	@PostConstruct
	public void init() {
		this.startupTime = Date.from(Instant.now());
	}

	@GetMapping("/health")
	public HealthData getHealth() {
		return new HealthData(
			"\uD83D\uDC4D", // thumbs-up emoji
			this.startupTime
		);
	}
}
