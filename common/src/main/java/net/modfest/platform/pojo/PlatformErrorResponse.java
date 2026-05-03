package net.modfest.platform.pojo;

import com.google.gson.JsonElement;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The standard way that platform returns errors
 */
public record PlatformErrorResponse(
	@NonNull ErrorType type,
	@NonNull JsonElement data,
	@Nullable Integer overrideStatusCode
) {
	public enum ErrorType {
		/**
		 * An id was provided, but that id doesn't exist. {@code data} will be of type {@link DoesntExist}
		 */
		DOESNT_EXIST(400),
		/**
		 * A submision id was provided, but it doesn't exist. {@code data} will be of type {@link SubmissionNoExist}
		 */
		SUBMISSION_NO_EXIST(400),
		/**
		 * Tried to remove a minecraft account, but it was already gone. {@code data} will be null
		 */
		MC_ALREADY_DELETED(400),
		/**
		 * Tried to update a submission that didn't have modrinth data. {@code data} will be null
		 */
		UPDATE_NON_MODRINTH(400),
		/**
		 * An attempt was made to use an id that was already used. {@code data} will be of type {@link AlreadyExists}
		 */
		ALREADY_USED(400),
		/**
		 * Any error with missing permissions. {@code data} will be a string with further details.
		 */
		PERMISSION_ERROR(401),
		/**
		 * For any *abnormal* and unexpected error. {@code data} will be a string with details about the error.
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

	public enum IdType {
		EVENT,
		MFUSER,
		MRUSER,
		MCACCOUNT,
		MRPROJECT,
	}

	public record DoesntExist(
		IdType type,
		String id
	) {}

	public record SubmissionNoExist(
		String eventid,
		String subid
	) {}

	@Override
	public String toString() {
		return type+": "+data;
	}
}
