package net.modfest.platform.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Ensures errors are handled even if they're thrown in filters
 * https://stackoverflow.com/questions/34595605/how-to-manage-exceptions-thrown-in-filters-in-spring
 */
@Controller
public class PlatformExceptionHandlerEnforcer implements ErrorController {
	@RequestMapping("/error")
	public ResponseEntity<Void> handleError(HttpServletRequest request) throws Throwable {
		var e = request.getAttribute("jakarta.servlet.error.exception");
		if (e != null) {
			if (e instanceof ServletException se && se.getCause() != null) {
				throw se.getCause();
			}
			throw (Throwable)e;
		} else {
			throw new IllegalStateException();
		}
	}
}
