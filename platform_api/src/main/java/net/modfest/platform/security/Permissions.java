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
}
