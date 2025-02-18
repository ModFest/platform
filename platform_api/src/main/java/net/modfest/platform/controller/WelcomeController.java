package net.modfest.platform.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WelcomeController {
	@Autowired
	private ApplicationContext applicationContext;

	/**
	 * A simple route over at the root of the domain, just for people who stumble
	 * upon this api and quickly want some info on what it is and how it works
	 */
	@GetMapping("/")
	public WelcomeReport welcome(HttpServletRequest request) {
		return new WelcomeReport(
			"Welcome wanderer!",
			getDocumentationUrl(request),
			applicationContext.getId(),
			applicationContext.getEnvironment().getProperty("build.version")
		);
	}

	private String getDocumentationUrl(HttpServletRequest request) {
		var baseUrl = request.getRequestURL().toString();
		var swaggerPath = applicationContext.getEnvironment().getProperty("springdoc.swagger-ui.path");
		if (swaggerPath == null) {
			swaggerPath = "/swagger-ui/index.html"; // Default location
		}
		// Remove double slash
		if (baseUrl.endsWith("/") && swaggerPath.startsWith("/")) {
			return baseUrl.substring(0, baseUrl.length()-1) + swaggerPath;
		} else {
			return baseUrl + swaggerPath;
		}
	}

	public record WelcomeReport(String about, String documentation, String name, String version) {

	}
}
