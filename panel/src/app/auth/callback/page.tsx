"use client"

import { useEffect, useState } from "react";
import { getRedirectUrl } from "../auth";
import { getToken } from "./server_handler";
import { ModfestAuth } from "@/auth_context";

export default function Home() {
	const [failed, setFailed] = useState(false)

	useEffect(() => {
		authWithModrinthOAuth(setFailed)
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

async function authWithModrinthOAuth(setFailed: (v: boolean) => void) {
	const urlParams = new URLSearchParams(window.location.search);
	const code = urlParams.get("code")

	var modrinthToken = await getToken(code!, getRedirectUrl())
	if (!("access_token" in modrinthToken) || !("expires_in" in modrinthToken)) {
		setFailed(true)
		return
	}

	const token = new ModfestAuth(modrinthToken["access_token"], modrinthToken["expires_in"])
	token.saveLocalStorage()

	// TODO redirect anywhere on site
	window.location.replace(window.location.origin)
}
