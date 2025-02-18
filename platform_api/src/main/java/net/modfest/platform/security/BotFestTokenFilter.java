package net.modfest.platform.security;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import net.modfest.platform.security.token.BotFestToken;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.servlet.AdviceFilter;
import org.apache.shiro.web.util.WebUtils;

public class BotFestTokenFilter extends AdviceFilter {
	protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {
 		var token = createToken(request, response);

		if (token != null) {
			var subject = SecurityUtils.getSubject();
			subject.login(token);
		}
		return true;
	}

	protected AuthenticationToken createToken(ServletRequest request, ServletResponse servletResponse) {
		var httpRequest = WebUtils.toHttp(request);

		var botfestSecret = httpRequest.getHeader("BotFest-Secret");
		if (botfestSecret != null) {
			var discordUser = httpRequest.getHeader("BotFest-Target-User");
			return new BotFestToken(botfestSecret, discordUser);
		}
		return null;
	}
}
