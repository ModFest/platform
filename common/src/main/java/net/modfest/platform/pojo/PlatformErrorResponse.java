package net.modfest.platform.pojo;

import com.google.gson.JsonElement;
import org.jspecify.annotations.NonNull;

/**
 * The standard way that platform returns errors
 */
public record PlatformErrorResponse(
	@NonNull ErrorType type,
	@NonNull JsonElement data
) {
	public enum ErrorType {
		/**
		 * An event was provided, but it doesn't exist. {@code data} will be a string of the provided event
		 */
		EVENT_NO_EXIST(400),
		/**
		 * A user was provided, but it doesn't exist. {@code data} will be a string of the provided user
		 */
		USER_NO_EXIST(400),
		/**
		 * An attempt was made to use an id that was already used. {@code data} will an of type {@link AlreadyExists}
		 */
		ALREADY_USED(400),
		/**
		 * For any *abnormal* and unexpected error
		 */
		INTERNAL(500);

		public final int httpStatus;

		ErrorType(int httpStatus) {
			this.httpStatus = httpStatus;
		}
	}

	public record AlreadyExists(
		/**
		 * The field which is already used (eg: modrinth or discord)
		 */
		String fieldName,
		/**
		 * The content of that field (eg: a modrinth or discord id)
		 */
		String content
	) {}

	@Override
	public String toString() {
		return type+": "+data;
	}
}
