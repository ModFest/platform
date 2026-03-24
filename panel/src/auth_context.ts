"use client"

const LOCALSTORAGE_KEY = "mr-token"

export class ModfestAuth {
	/**
	 * A modrinth token, which platform accepts as a valid means of authentication
	 */
	private mrToken: string
	/**
	 * Unix timestamp (seconds since 1970)
	 */
	private validUntil: number

	public constructor(mrToken: string, validUntil: number) {
		this.mrToken = mrToken
		this.validUntil = validUntil
	}

	static readLocalStorage(): ModfestAuth | undefined {
		const token = localStorage.getItem(LOCALSTORAGE_KEY)
		if (token == null) {
			return undefined
		}
		const tokenData = JSON.parse(token);
		const mrToken = tokenData["access_token"]
		if (typeof mrToken !== "string") {
			return undefined
		}
		var validUntil = tokenData["valid_until"]
		if (typeof validUntil !== "number") {
			return undefined
		}
	
		return new ModfestAuth(mrToken, validUntil)
	}

	public saveLocalStorage() {
		localStorage.setItem(LOCALSTORAGE_KEY, JSON.stringify({
			"access_token": this.mrToken,
			"valid_until": this.validUntil,
		}));
	}

	/**
	 * The modfest auth data
	 */
	public isValid(): boolean {
		return this.validUntil > (Date.now() / 1000)
	}

	public configureFetch(): {headers: Record<string, string>} {
		return {
			headers: {
				"Modrinth-Token": this.mrToken
			}
		}
	}
}

export function logout() {
	localStorage.removeItem(LOCALSTORAGE_KEY)
}

export function readAuthData(): ModfestAuth | undefined {
	return ModfestAuth.readLocalStorage()
}
