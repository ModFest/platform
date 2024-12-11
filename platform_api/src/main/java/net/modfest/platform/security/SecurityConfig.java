package net.modfest.platform.security;


import org.apache.shiro.realm.Realm;
import org.apache.shiro.spring.web.config.DefaultShiroFilterChainDefinition;
import org.apache.shiro.spring.web.config.ShiroFilterChainDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @see <a href="https://shiro.apache.org/spring-framework.html">Integrating Shiro into Spring-based Applications</a>
 * @see <a href="https://www.mo4tech.com/springboot-series-7-ways-to-use-shiro-custom-filters-and-tokens-in-springboot-projects.html">Ways to use Shiro custom filters and tokens in Springboot projects</a>
 */
@Configuration
//@Import({ShiroBeanConfiguration.class,
//	ShiroAnnotationProcessorConfiguration.class,
//	ShiroWebConfiguration.class,
//	ShiroWebFilterConfiguration.class,
//	ShiroRequestMappingConfig.class})
public class SecurityConfig {
//	@Primary
//	@Bean
//	public ShiroFilterFactoryBean shiroFilter(SecurityManager defaultWebSecurityManager, ShiroFilterChainDefinition filterChain) {
//		var filter = new ShiroFilterFactoryBean();
//		filter.setSecurityManager(defaultWebSecurityManager);
//		filter.setFilters(Map.of(
//				"botfest", new BotFestTokenFilter()
//		));
//		filter.setFilterChainDefinitionMap(filterChain.getFilterChainMap());
//		return filter;
//	}

	@Bean
	public Realm realm() {
		return new ModFestRealm();
	}

//	@Primary
//	@Bean(name = "customFilter")
	@Bean
	public ShiroFilterChainDefinition shiroFilterChainDefinition() {
		DefaultShiroFilterChainDefinition chainDefinition = new DefaultShiroFilterChainDefinition();
		chainDefinition.addPathDefinition("/**", "anon[permissive]");
		return chainDefinition;
	}

	@Bean(name = "botfest")
	public BotFestTokenFilter customFilter() {
		return new BotFestTokenFilter();
	}

//	@Bean(
//			name = {"globalFilters"}
//	)
//	protected List<String> globalFilters() {
//		return Collections.singletonList(DefaultFilter.invalidRequest.name());
//	}
}
