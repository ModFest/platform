"use client"

import { createContext, useContext, useEffect, useState } from "react";
import { ModfestAuth } from "./auth_context";
import { createEventSource } from "eventsource-client";
import { CurrentEventData, EventData, ScheduleEntryData, UserData } from "./platform_types";

const PLATFORM = getPlatformUrl()!

function getPlatformUrl(): string | undefined {
	if (process.env.NEXT_PUBLIC_PLATFORM_API) {
		return process.env.NEXT_PUBLIC_PLATFORM_API
	}
	if (process.env.NODE_ENV === "development" && process.env.DEV_SERVER_URL) {
		return process.env.DEV_SERVER_URL
	}
}

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
			headers: {
				"Content-Type": "application/json",
				...this.auth.configureFetch().headers
			},
		}).then(throwIfNotOk)
	}

	public async getCurrentEvent(): Promise<CurrentEventData> {
		return fetch(`${PLATFORM}/currentevent/`).then(throwIfNotOk).then(r => r.json())
	}

	public useEvent(eventid: string): EventData | undefined {
		const [event, setEvent] = useState<EventData | undefined>(undefined)
		useEffect(() => {
			fetch(`${PLATFORM}/event/${eventid}`)
				.then(throwIfNotOk)
				.then(r => r.json())
				.then(d => setEvent(d))
		}, [this, eventid])
		return event
	}

	public useEventSchedule(eventid: string): ScheduleEntryData[] | undefined {
		const [schedule, setSchedule] = useState<ScheduleEntryData[] | undefined>(undefined)
		useEffect(() => {
			fetch(`${PLATFORM}/event/${eventid}/schedule`)
				.then(throwIfNotOk)
				.then(r => r.json())
				.then(d => setSchedule(d))
		}, [this, eventid])
		return schedule
	}
}

function throwIfNotOk(r: Response): Response {
	if (!r.ok) {
		throw r
	}
	return r;
}
