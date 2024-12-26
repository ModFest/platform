package net.modfest.platform.security;

import net.modfest.platform.pojo.UserData;
import org.apache.shiro.subject.Subject;

import java.util.Objects;

public class PermissionUtils {
	/**
	 * @return True if the given subject owns the provided data
	 */
	public static boolean owns(Subject subject, UserData data) {
		var principal = subject.getPrincipal();

		switch (principal) {
			case UserData user -> {
				return Objects.equals(user.id(), data.id());
			}
			case null, default -> {
				return false;
			}
		}
	}
}
