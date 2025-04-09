package net.modfest.platform.controller;

import com.google.gson.Gson;
import net.modfest.platform.misc.PlatformStandardException;
import net.modfest.platform.pojo.PlatformErrorResponse;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.lang.ShiroException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * This class is responsible for mapping internal java exceptions to
 * http responses
 */
@ControllerAdvice
public class PlatformExceptionHandler {
	@Autowired
	private Gson gson;

	/**
	 * Forwards any exception that uses {@link PlatformStandardException}
	 */
	@ExceptionHandler(PlatformStandardException.class)
	private ResponseEntity<PlatformErrorResponse> platformException(PlatformStandardException t) {
		return toResponse(new PlatformErrorResponse(
			t.getType(),
			gson.toJsonTree(t.getData())
		));
	}

	@ExceptionHandler(UnauthenticatedException.class)
	private ResponseEntity<PlatformErrorResponse> unauthenticatedException(UnauthenticatedException e) {
		return toResponse(new PlatformErrorResponse(
			PlatformErrorResponse.ErrorType.PERMISSION_ERROR,
			gson.toJsonTree("user is not logged in")
		));
	}

	@ExceptionHandler({AuthorizationException.class, AuthenticationException.class})
	private ResponseEntity<PlatformErrorResponse> authorizationException(ShiroException e) {
		return toResponse(new PlatformErrorResponse(
			PlatformErrorResponse.ErrorType.PERMISSION_ERROR,
			gson.toJsonTree(e.getMessage())
		));
	}

	/**
	 * Catch-all for any exception not caught by anything more specific
	 */
	@ExceptionHandler(Throwable.class)
	private ResponseEntity<PlatformErrorResponse> anyError(Throwable t) {
		if (t instanceof ErrorResponse e) {
			return new ResponseEntity<>(
				new PlatformErrorResponse(
					PlatformErrorResponse.ErrorType.INTERNAL,
					gson.toJsonTree(e.getBody())
				),
				e.getStatusCode()
			);
		}
		t.printStackTrace();
		return toResponse(new PlatformErrorResponse(
			PlatformErrorResponse.ErrorType.INTERNAL,
			gson.toJsonTree(t.getMessage())
		));
	}

	private static ResponseEntity<PlatformErrorResponse> toResponse(PlatformErrorResponse errorResponse) {
		return new ResponseEntity<>(
			errorResponse,
			HttpStatusCode.valueOf(errorResponse.type().httpStatus)
		);
	}
}
