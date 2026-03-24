"use client"

import { useEffect, useState } from "react";
import { getCallbackUrl, getOauthBrowserKey } from "../auth";
import { getToken } from "./server_handler";
import { ModfestAuth } from "@/auth_context";
import { useRouter } from "next/navigation";
import { AppRouterInstance } from "next/dist/shared/lib/app-router-context.shared-runtime";

// This page completes the OAUTH flow. This is the page where modrinth redirects the user to once they've
// logged in

export default function Home() {
	const router = useRouter()
	const [failed, setFailed] = useState(false)

	useEffect(() => {
		authWithModrinthOAuth(router, setFailed)
	}, [])

	if (failed) {
		return <main>
			<center>
				<h1>Authentication failed</h1>
				<a href="./login">Try again</a>
			</center>
		</main>
	}

	return (
		<main>
			<center>
				<h1>Authenticating...</h1>
			</center>
		</main>
	);
}

async function authWithModrinthOAuth(router: AppRouterInstance, setFailed: (v: boolean) => void) {
	const urlParams = new URLSearchParams(window.location.search);
	const code = urlParams.get("code")
	const state = urlParams.get("state")

	if (!code || !state) {
		setFailed(true)
		return
	}

	const [securityToken, redirect] = state.split(".", 2)
	if (securityToken !== getOauthBrowserKey()) {
		setFailed(true)
		return
	}

	var modrinthToken = await getToken(code!, getCallbackUrl())
	if (!("access_token" in modrinthToken) || !("expires_at" in modrinthToken)) {
		setFailed(true)
		return
	}

	const token = new ModfestAuth(modrinthToken["access_token"], modrinthToken["expires_at"])
	token.saveLocalStorage()

	redirectBack(router, redirect)
}

export function redirectBack(router: AppRouterInstance, url: string) {
	// Quite important, this url is untrusted and we don't want to be redirecting to arbitrary pages
	if (!url.startsWith("/")) {
		url = "/"
	}
	// We should never redirect back to the authentication flow, that'll give infinite loops
	if (url.startsWith("/auth")) {
		url = "/"
	}
	router.replace(url)
}
