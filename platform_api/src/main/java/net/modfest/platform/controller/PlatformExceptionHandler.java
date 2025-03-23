package net.modfest.platform.controller;

import com.google.gson.Gson;
import net.modfest.platform.misc.PlatformStandardException;
import net.modfest.platform.pojo.PlatformErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

	@ExceptionHandler(NoResourceFoundException.class)
	private ResponseEntity<PlatformErrorResponse> onNotFound(NoResourceFoundException notFound) {
		// This defines how 404's are handled. We'll give them the internal/other type but with a 404 code instead of 500
		return new ResponseEntity(new PlatformErrorResponse(
			PlatformErrorResponse.ErrorType.INTERNAL,
			gson.toJsonTree(notFound.getMessage())
		), HttpStatusCode.valueOf(404));
	}

	/**
	 * Catch-all for any exception not caught by anything more specific
	 */
	@ExceptionHandler(Throwable.class)
	private ResponseEntity<PlatformErrorResponse> anyError(Throwable t) {
		if (t instanceof ResponseStatusException e) {
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
