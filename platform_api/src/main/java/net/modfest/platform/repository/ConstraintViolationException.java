package net.modfest.platform.repository;

public class ConstraintViolationException extends RuntimeException {
	public ConstraintViolationException(String message) {
		super(message);
	}
}
