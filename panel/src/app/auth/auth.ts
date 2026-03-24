"use client"

export function getCallbackUrl(): string {
	return `${window.location.origin}/auth/callback`
} 

/**
 * Random value which is generated per-browser and then stored in localStorage.
 * Only ever generated once (unless the user clears their cache).
 * 
 * See the comments in the login page for more information
 * 
 * The resulting value is guaranteed to be alphanumeric
 */
export function getOauthBrowserKey(): string {
	let key = localStorage.getItem("oauthbrowserkey");
	if (!key) {
		key = crypto.randomUUID().replace("-", "");
		localStorage.setItem("oauthbrowserkey", key);
	}
	return key;
}
