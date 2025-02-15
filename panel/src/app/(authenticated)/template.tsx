"use client"

import { readAuthData, logout } from "@/auth_context"
import { Platform, PlatformContext } from "@/platform";
import { useRouter } from "next/navigation"
import { useEffect, useReducer } from "react";

export default function Template({ children }: { children: React.ReactNode }) {
	const router = useRouter()
	const auth = readAuthData()
	// Hack to be able to force updates
	const [, forceUpdate] = useReducer(x => x + 1, 0);

	useEffect(() => {
		if (!auth) {
			router.push("/auth/login")
		}
	})
	if (!auth) {
		return <main>
			<h1>Redirecting...</h1>
		</main>
	}
	
	const logoutButton = () => {
		logout()
		forceUpdate()
	}

	const platform = Platform.new(auth)
	return <PlatformContext.Provider value={platform}>
		<main>
			<button onClick={logoutButton}>Log out</button>
			{children}
		</main>
	</PlatformContext.Provider>
}
