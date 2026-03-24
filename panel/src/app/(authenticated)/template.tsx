"use client"

import { readAuthData, logout, ModfestAuth } from "@/auth_context"
import { Platform, PlatformContext } from "@/platform";
import { usePathname, useRouter, useSearchParams } from "next/navigation"
import { createContext, use, useContext, useEffect, useReducer, useState } from "react";

export default function Template({ children }: { children: React.ReactNode }) {
	const [auth, setAuth] = useState<ModfestAuth | undefined>(undefined)
	const router = useRouter()
	const [platformCache, setPlatformCache] = useState({});

	useEffect(() => {
		if (!auth) {
			const a = readAuthData()
			if (a && a.isValid()) {
				setAuth(a)
			} else {
				var currentUrl = window.location.pathname + window.location.search + window.location.hash
				router.push(`/auth/login?r=${encodeURIComponent(currentUrl)}`)
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

	const platform = Platform.new(auth, platformCache, setPlatformCache)
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
