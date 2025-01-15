package net.modfest.platform.security;

public class Permissions {
	/**
	 * Permissions for controlling the platform itself, rather than any data on platform
	 */
	public static class Meta {
		/**
		 * The ability to invalidate caches and reload data. Shouldn't be
		 * destructive but might be resource intensive
		 */
		public static final String RELOAD = "meta.reload";
	}

	public static class Users {
		public static final String LIST_ALL = "users.list";
		/**
		 * Allows the user to edit all other users their data
		 */
		public static final String EDIT_OTHERS = "users.edit_others";
		/**
		 * Permission to create new users
		 */
		public static final String CREATE = "users.create";
	}
}
