package net.modfest.platform.configuration;

import net.modfest.platform.git.GitRequestInterceptor;
import net.modfest.platform.git.GlobalGitManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.pattern.PathPatternParser;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
	@Autowired
	private GlobalGitManager git;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**");
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new GitRequestInterceptor(git));
	}

	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		configurer.setPatternParser(new PathPatternParser());
	}
}
