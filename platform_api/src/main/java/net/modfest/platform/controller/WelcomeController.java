package net.modfest.platform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WelcomeController {
	@Autowired
	private ApplicationContext applicationContext;

	/**
	 * A simple route over at the root of the domain. Not really part of the api,
	 * but useful to see if the api is running correctly.
	 */
	@GetMapping("/")
	public WelcomeReport welcome() {
		return new WelcomeReport(
			"Welcome wanderer!",
			"https://github.com/ModFest/platform",
			applicationContext.getId(),
			applicationContext.getEnvironment().getProperty("build.version")
		);
	}

	public record WelcomeReport(String about, String source, String name, String version) {

	}
}
