package net.modfest.platform.git;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.modfest.platform.pojo.UserData;
import net.modfest.platform.security.BotFestIdentity;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class GitRequestInterceptor implements AsyncHandlerInterceptor {
	private static final Logger LOGGER = LoggerFactory.getLogger(GitRequestInterceptor.class);
	private final GlobalGitManager git;

	public GitRequestInterceptor(GlobalGitManager git) {
		this.git = git;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
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
		return true;
	}

	private void onClose() {
		try {
			git.closeScope();
		} catch (Exception e) {
			// We can't throw exceptions at this point, it'll mess up the response
			LOGGER.error("Error during finalization of request", e);
		}
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		onClose();
	}

	@Override
	public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		onClose();
	}
}
