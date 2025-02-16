"use client"

export const LOCALSTORAGE_KEY = "mr-token"

export class ModfestAuth {
	private mrToken: string

	private constructor(mrToken: string) {
		this.mrToken = mrToken
	}

	static readAuthData(): ModfestAuth | undefined {
		const token = localStorage.getItem(LOCALSTORAGE_KEY)
		if (token == null) {
			return undefined
		}
		const tokenData = JSON.parse(token);
		const mrToken = tokenData["access_token"]
		if (typeof mrToken !== "string") {
			return undefined
		}
	
		return new ModfestAuth(mrToken)
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
	return ModfestAuth.readAuthData()
}
