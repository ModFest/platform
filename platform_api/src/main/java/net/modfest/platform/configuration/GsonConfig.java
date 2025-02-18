package net.modfest.platform.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.modfest.platform.gson.GsonCommon;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GsonConfig {
	// This gson instance will be provided to the rest of the application, as well as
	// spring itself
	@Bean
	public Gson configureGson() {
		var builder = new GsonBuilder();
		GsonCommon.configureGson(builder); // Cross-project gson configuration
		return builder.create();
	}
}
