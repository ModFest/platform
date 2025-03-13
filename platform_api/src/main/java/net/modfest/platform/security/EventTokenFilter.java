package net.modfest.platform.security;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import net.modfest.platform.security.token.EventToken;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.servlet.AdviceFilter;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.core.annotation.Order;

@Order(10)
public class EventTokenFilter extends AdviceFilter {
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

		var eventToken = httpRequest.getHeader("Mf-Event-Token");
		if (eventToken != null) {
			return new EventToken(eventToken);
		}
		return null;
	}
}
