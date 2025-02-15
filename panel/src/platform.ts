"use client"

import { createContext, useContext } from "react";
import { ModfestAuth } from "./auth_context";

export const PlatformContext = createContext<Platform | undefined>(undefined)

export function usePlatform(): Platform {
	return useContext(PlatformContext)!
}

export class Platform {
	private auth: ModfestAuth

	private constructor(auth: ModfestAuth) {
		this.auth = auth
	}

	static new(auth: ModfestAuth): Platform {
		return new Platform(auth)
	}

	public async getAllUsers(): Promise<any[]> {
		return fetch("http://localhost:8070/users", {
			...this.auth.configureFetch()
		}).then(r => r.json())
	}
}
