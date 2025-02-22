"use server"

import { getModrinthApi, getModrinthAppId } from "@/env_vars"

export async function getToken(code: string, redirect_uri: string): Promise<ModrinthToken> {
	// This function is run on the server, it needs access to the client secret
	const modrinthApi = getModrinthApi()

	var result = await fetch(`${modrinthApi}/_internal/oauth/token`, {
		method: "POST",
		headers: {
			"Content-Type": "application/x-www-form-urlencoded",
			"Authorization": `Bearer ${process.env.MODRINTH_APP_SECRET}`
		},
		body: new URLSearchParams({
			code: code!,
			client_id: getModrinthAppId()!,
			redirect_uri: redirect_uri,
			grant_type: "authorization_code"
		})
	})
	return <ModrinthToken>(await result.json())
}

export type ModrinthToken = {
	access_token: string,
	token_type: string,
	expires_in: number
}
