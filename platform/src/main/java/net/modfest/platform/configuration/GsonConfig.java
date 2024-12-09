package net.modfest.platform.configuration;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import net.modfest.platform.configuration.json.CustomDateDeserializer;
import net.modfest.platform.configuration.json.EnumToLowerCaseJsonConverter;
import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Date;

@Configuration
public class GsonConfig {
	// Spring will call this bean to configure its gson instance
	@Bean
	public GsonBuilderCustomizer configureGson() {
		return builder -> {
			builder
				.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
				.registerTypeHierarchyAdapter(Enum.class, new EnumToLowerCaseJsonConverter())
				.registerTypeAdapter(Date.class, new CustomDateDeserializer())
				.setPrettyPrinting()
				.setLenient();
		};
	}
}
