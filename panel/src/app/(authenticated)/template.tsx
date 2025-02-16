"use client"

import { readAuthData, logout, ModfestAuth } from "@/auth_context"
import { Platform, PlatformContext } from "@/platform";
import { useRouter } from "next/navigation"
import { use, useEffect, useReducer, useState } from "react";

export default function Template({ children }: { children: React.ReactNode }) {
	const [auth, setAuth] = useState<ModfestAuth | undefined>(undefined)
	const router = useRouter()
	// Hack to be able to force updates
	const [, forceUpdate] = useReducer(x => x + 1, 0);

	useEffect(() => {
		const a = readAuthData()
		if (!a) {
			router.push("/auth/login")
		} else {
			setAuth(a)
		}
	}, [])
	if (!auth) {
		return <main>
			<h1>Loading...</h1>
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
