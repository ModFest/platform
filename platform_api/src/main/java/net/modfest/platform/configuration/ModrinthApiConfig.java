package net.modfest.platform.configuration;

import nl.theepicblock.dukerinth.ModrinthApi;
import nl.theepicblock.dukerinth.UserAgentBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ModrinthApiConfig {
	@Bean
	public ModrinthApi getModrinthApi(@Autowired Environment environment) {
		var name = environment.getProperty("spring.application.name");
		var version = environment.getProperty("build.version");
		var contactUrl = environment.getProperty("project.url");

		return new ModrinthApi(new UserAgentBuilder(name).version(version).contactUrl(contactUrl));
	}
}
