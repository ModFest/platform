"use server"

export async function getToken(code: string, redirect_uri: string): Promise<ModrinthTokenData> {
	// This function is run on the server, it needs access to the client secret
	const modrinthApi = process.env.NEXT_PUBLIC_MODRINTH_API

	var result = await fetch(`${modrinthApi}/_internal/oauth/token`, {
		method: "POST",
		headers: {
			"Content-Type": "application/x-www-form-urlencoded",
			"Authorization": `Bearer ${process.env.MODRINTH_APP_SECRET}`
		},
		body: new URLSearchParams({
			code: code!,
			client_id: process.env.NEXT_PUBLIC_MODRINTH_APP_ID!,
			redirect_uri: redirect_uri,
			grant_type: "authorization_code"
		})
	})
	const data = <ModrinthToken>(await result.json())
	return {
		access_token: data.access_token,
		token_type: data.token_type,
		// Subtract 5 minutes to account for latency between us and modrinth
		expires_at: (Date.now() / 1000) + data.expires_in - 60*5
	}
}

export type ModrinthToken = {
	access_token: string,
	token_type: string,
	expires_in: number
}

export type ModrinthTokenData = {
	access_token: string,
	token_type: string,
	expires_at: number
}
