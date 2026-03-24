"use client"
import { readAuthData } from "@/auth_context";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { getOauthBrowserKey, getCallbackUrl } from "../auth";
import { redirectBack } from "../callback/page";


export default function Inner(props: {redirect_url: string}) {
	const router = useRouter()
	const redirect = props.redirect_url
	// We redirect the user to OAUTH themselves with modrinth
	// This is documented on https://docs.modrinth.com/guide/oauth/
	// The flow is as follows:
	//  * The user tries to access an authenticated page, they do not have a token in localStorage
	//  * The user is redirected to this page, the previous page they were on is stored in a query parameter named `r`
	//  * The user clicks the "log in with Modrinth" button, and gets sent to a modrinth page
	//  * The user completes the login in modrinth, and is redirected to our special "callback" page
	//  * The callback page reads the code which modrinth passed through in a query parameter
	//  * The callback page sends this code to the server, where it's combined with an application secret key to
	//    obtain a modrinth token
	//  * The callback page reads the `r` query parameter and redirects back to the original page

	// We want to check if the user got sent here erroneously,
	// aka if they got authenticated in the meantime
	const checkLoggedIn = useCallback(() => {
		const a = readAuthData()
		if (a && a.isValid()) {
			redirectBack(router, redirect)
		}
	}, [router, redirect]);
	useEffect(() => checkLoggedIn(), [checkLoggedIn]); // Wrapped in useEffect so it only runs on client

	// We also want to keep an eye on localStorage. It might be that the user has multiple tabs open,
	// and logged in on aNextRouter different tab.
	useEffect(() => {
		const controller = new AbortController();
		addEventListener("storage", () => {
			checkLoggedIn()
		}, { signal: controller.signal })
		return () => controller.abort()
	}, [checkLoggedIn])

	// These need to be in a useEffect because they must run on the client and not the server
	const [oauthBrowserKey, setOauthBrowserKey] = useState<string | undefined>(undefined)
	useEffect(() => {
		if (!oauthBrowserKey) {
			setOauthBrowserKey(getOauthBrowserKey())
		}
	}, [oauthBrowserKey]);

	const [callbackUrl, setCallbackUrl] = useState<string | undefined>(undefined)
	useEffect(() => {
		if (!callbackUrl) {
			setCallbackUrl(getCallbackUrl())
		}
	}, [callbackUrl]);

	if (!callbackUrl) {
		return <main>
			<center>
				<h1>ModFest panel</h1>
				Log in with Modrinth
			</center>
		</main>
	}
	
	// We can provide a "state" parameter to modrinth. This will be passed on unmodified to the callback
	// We use it for two reasons. Firstly, we insert a random browser-specific value into it.
	// If we didn't do this, anyone could create a link to our callback page. When someone clicks
	// that link the callback will blindly assume the token is correct and it'll just be a
	// confusing mess. So we store some random value in localStorage, and any attacker won't be
	// able to guess that.
	// Secondly, we use the state variable to encode the return path. 
	const state = oauthBrowserKey+"."+redirect

	const queryParams = {
		// This is the only auth type modrinth supports
		"response_type": "code",
		// Identifier for the modrinth app
		"client_id": process.env.NEXT_PUBLIC_MODRINTH_APP_ID!,
		// We only want the bare minimum scope, this should also be set in the modrinth application's settings (https://modrinth.com/settings/applications)
		"scope": "USER_READ",
		// The state variable (which will be passed on unmodified)
		"state": state,
		// The url for our callback page
		"redirect_uri": callbackUrl,
	};

  	const oathUrl = `${process.env.NEXT_PUBLIC_MODRINTH_SITE}/auth/authorize?` + Object.entries(queryParams).map(([k,v]) => k+"="+encodeURIComponent(v)).join("&")

	return (
		<main>
			<center>
				<h1>ModFest panel</h1>
				<a href={oathUrl}>Log in with Modrinth</a>
			</center>
		</main>
	);
}
