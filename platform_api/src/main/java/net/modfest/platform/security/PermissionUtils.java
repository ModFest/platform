package net.modfest.platform.security;

import net.modfest.platform.pojo.SubmissionData;
import net.modfest.platform.pojo.UserData;
import org.apache.shiro.subject.Subject;

import java.util.Objects;

public class PermissionUtils {
	/**
	 * @return True if the given subject owns the provided data
	 */
	public static boolean owns(Subject subject, UserData data) {
		return isUser(subject, data.id());
	}

	private static boolean isUser(Subject subject, String userId) {
		var principal = subject.getPrincipal();

		switch (principal) {
			case UserData user -> {
				return Objects.equals(user.id(), userId);
			}
			case null, default -> {
				return false;
			}
		}
	}

	/**
	 * @return True if the given subject owns the provided data
	 */
	public static boolean owns(Subject subject, SubmissionData data) {
		return data.authors().stream().anyMatch(a -> isUser(subject, a));
	}
}
