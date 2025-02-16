"use client"

import { createContext, useContext, useEffect, useState } from "react";
import { ModfestAuth } from "./auth_context";
import { createEventSource } from "eventsource-client";
import { UserData, UserPatchData } from "./platform_types";

const PLATFORM = process.env.NEXT_PUBLIC_PLATFORM_API!

export const PlatformContext = createContext<Platform | undefined>(undefined)

export function usePlatform(): Platform {
	return useContext(PlatformContext)!
}

export class Platform {
	private readonly auth: ModfestAuth

	private constructor(auth: ModfestAuth) {
		this.auth = auth
	}

	static new(auth: ModfestAuth): Platform {
		return new Platform(auth)
	}

	public useAllUsers(): UserData[] {
		const [users, setUsers] = useState<UserData[]>([])
		var usersCache = users
		useEffect(() => {
			const refetchAllUsers = () => {
				// Resync completely
				fetch(`${PLATFORM}/users`, this.auth.configureFetch()).then(d => d.json()).then(data => {
					usersCache = data
					setUsers(data)
				})
			}

			const sse = createEventSource({
				url: `${PLATFORM}/users/subscribe`,
				onConnect: refetchAllUsers,
				onMessage: (event) => {
					const userId = event.data
					fetch(`${PLATFORM}/user/${userId}`, this.auth.configureFetch()).then(d => d.json()).then(newUser => {
						for (var i = 0; i < usersCache.length; i++) {
							if (usersCache[i].id === userId) {
								const newData = [...usersCache]
								if (newUser) {
									newData[i] = newUser
									usersCache = newData
									setUsers(newData)
								} else {
									newData.splice(i, 1)
									usersCache = newData
									setUsers(newData)
								}
								return
							}
						}
						// User did not previously exist
						const newData = [newUser, ...usersCache]
						usersCache = newData
						setUsers(newData)
					})
				},
				...this.auth.configureFetch()
			})

			return () => sse.close()
		}, [this])
		return users
	}

	public async updateUser(d: UserData) {
		return fetch(`${PLATFORM}/admin/update_user`, {
			method: "POST",
			body: JSON.stringify(d),
			...this.auth.configureFetch()
		}).then(r => {
			if (!r.ok) {
				throw r
			}
			return r;
		})
	}
}
