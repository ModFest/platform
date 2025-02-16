"use client"

import { readAuthData, logout, ModfestAuth } from "@/auth_context"
import { Platform, PlatformContext } from "@/platform";
import { useRouter } from "next/navigation"
import { createContext, use, useContext, useEffect, useReducer, useState } from "react";

export default function Template({ children }: { children: React.ReactNode }) {
	const [auth, setAuth] = useState<ModfestAuth | undefined>(undefined)
	const router = useRouter()
	// Hack to be able to force updates
	const [, forceUpdate] = useReducer(x => x + 1, 0);

	useEffect(() => {
		if (!auth) {
			const a = readAuthData()
			if (!a) {
				router.push("/auth/login")
			} else {
				setAuth(a)
			}
		}
	}, [auth])
	if (!auth) {
		return <main>
			<h1>Loading...</h1>
		</main>
	}
	
	const logoutCtx = {
		onLogout: () => {
			logout()
			setAuth(undefined)
		}
	}

	const platform = Platform.new(auth)
	return <PlatformContext.Provider value={platform}>
		<LogoutCtx.Provider value={logoutCtx}>
			{children}
		</LogoutCtx.Provider>
	</PlatformContext.Provider>
}

const LogoutCtx = createContext<LogoutCtx | undefined>(undefined)
type LogoutCtx = {
	onLogout: () => void
};

export function useLogout(): () => void {
	return useContext(LogoutCtx)?.onLogout!
}
