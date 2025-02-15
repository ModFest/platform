"use client"

import { useEffect, useState } from "react";
import { getRedirectUrl } from "../auth";
import { getToken } from "./server_handler";

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

	var token = await getToken(code!, getRedirectUrl())
	if (!("access_token" in token)) {
		// Fail!
		setFailed(true)
		return
	}

	localStorage.setItem("mr-token", JSON.stringify(token))

	// TODO redirect anywhere on site
	window.location.replace(window.location.origin)
}
