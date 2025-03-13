package net.modfest.platform.git;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import net.modfest.platform.pojo.UserData;
import net.modfest.platform.security.BotFestIdentity;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.web.servlet.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(20) // This causes it to be ordered after the BotFest/ModrinthTokenFilter's which do auth
public class GitRequestInterceptor extends OncePerRequestFilter {
	private static final Logger LOGGER = LoggerFactory.getLogger(GitRequestInterceptor.class);
	@Autowired
	private GlobalGitManager git;

	@Override
	protected void doFilterInternal(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws ServletException, IOException {
		try {
			if (servletRequest instanceof HttpServletRequest httpRequest) {
				preHandle(httpRequest);
			}
			filterChain.doFilter(servletRequest, servletResponse);
		} finally {
			if (servletRequest instanceof HttpServletRequest) {
				postHandle();
			}
		}
	}

	private void preHandle(HttpServletRequest request) {
		var auth = SecurityUtils.getSubject();
		var principal = auth.getPrincipal();
		var userString = switch (principal) {
			case UserData user -> "ModFest user "+user.id()+" ("+user.name()+")";
			case BotFestIdentity i -> "BotFest user";
			case null -> "Unauthenticated";
			default -> throw new IllegalStateException("Unexpected value: " + principal);
		};

		var gitScope = new GitScope("""
			%s %s
			
			Auth: %s
			""".formatted(request.getMethod(), request.getRequestURI(), userString));
		git.setScope(gitScope);
	}

	private void postHandle() {
		try {
			git.closeScope();
		} catch (Exception e) {
			// We can't throw exceptions at this point, it'll mess up the response
			LOGGER.error("Error during finalization of request", e);
		}
	}
}
