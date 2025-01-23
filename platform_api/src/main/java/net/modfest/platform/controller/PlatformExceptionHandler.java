package net.modfest.platform.controller;

import com.google.gson.Gson;
import com.google.gson.JsonPrimitive;
import net.modfest.platform.misc.PlatformStandardException;
import net.modfest.platform.pojo.PlatformErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
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

	/**
	 * Catch-all for any exception not caught by anything more specific
	 */
	@ExceptionHandler(Throwable.class)
	private ResponseEntity<PlatformErrorResponse> anyError(Throwable t) {
		return toResponse(new PlatformErrorResponse(
			PlatformErrorResponse.ErrorType.INTERNAL,
			new JsonPrimitive(t.getMessage())
		));
	}

	private static ResponseEntity<PlatformErrorResponse> toResponse(PlatformErrorResponse errorResponse) {
		return new ResponseEntity<>(
			errorResponse,
			HttpStatusCode.valueOf(errorResponse.type().httpStatus)
		);
	}
}
