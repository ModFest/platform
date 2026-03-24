"use client"

import { createContext, useContext, useEffect, useState } from "react";
import { ModfestAuth } from "./auth_context";
import { createEventSource } from "eventsource-client";
import { CurrentEventData, EventData, EventTokenData, ScheduleEntryData, SubmissionData, UserData } from "./platform_types";

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
	private readonly cachedState: Record<string, any>
	private readonly setCachedState: ((f: (o: Record<string, any>) => Record<string, any>) => void)

	private constructor(auth: ModfestAuth, cachedState: Record<string, any>, setCachedState: (f: (o: Record<string, any>) => Record<string, any>) => void) {
		this.auth = auth
		this.cachedState = cachedState;
		this.setCachedState = setCachedState;
	}

	static new(auth: ModfestAuth, cachedState: Record<string, any>, setCachedState: (f: (o: Record<string, any>) => Record<string, any>) => void): Platform {
		return new Platform(auth, cachedState, setCachedState)
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
		}, [this.auth])
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

	public async removeAuthorFromSubmission(eventId: string, submissionId: string, authorId: string) {
		fetch(`${PLATFORM}/event/${eventId}/submission/${submissionId}/authors/${authorId}`, {
			method: "DELETE"
		}).then(throwIfNotOk);
	}

	public useCurrentEvent(): CurrentEventData | undefined {
		const [user, setCurrentEvent] = useState<CurrentEventData | undefined>()
		useEffect(() => {
			fetch(`${PLATFORM}/currentevent/`)
				.then(throwIfNotOk)
				.then(r => r.json())
				.then(c => setCurrentEvent(c))
		}, [])
		return user;
	}

	public useUser(userId: string): UserData | undefined {
		const [user, setUser] = useState<UserData | undefined>()
		useEffect(() => {
			fetch(`${PLATFORM}/user/${userId}`)
				.then(throwIfNotOk)
				.then(r => r.json())
				.then(d => setUser(d))
		}, [this.auth, userId])
		return user;
	}

	public useAllEvents(): EventData[] | undefined {
		const [events, setEvents] = useState<EventData[] | undefined>(undefined)
		useEffect(() => {
			fetch(`${PLATFORM}/events`)
				.then(throwIfNotOk)
				.then(r => r.json())
				.then(d => setEvents(d))
		}, [this.auth])
		return events
	}

	public useEvent(eventid: string): EventData | undefined {
		const [event, setEvent] = useState<EventData | undefined>(undefined)
		useEffect(() => {
			fetch(`${PLATFORM}/event/${eventid}`)
				.then(throwIfNotOk)
				.then(r => r.json())
				.then(d => setEvent(d))
		}, [this.auth, eventid])
		return event
	}

	public useEventTokens(): EventTokenData | undefined {
		useEffect(() => {
			this.refetchEventTokens()
		}, [this.auth])
		return this.cachedState["event_tokens"]
	}

	private refetchEventTokens() {
		fetch(`${PLATFORM}/event_tokens/`, this.auth.configureFetch())
			.then(throwIfNotOk)
			.then(r => r.json())
			.then(d => this.setCachedState((state) => ({...state, "event_tokens": d})))
	}

	public regenerateToken(eventid: string) {
		fetch(`${PLATFORM}/event_tokens/${eventid}/regenerate`, {
			method: "POST",
			...this.auth.configureFetch()
		})
		.then(throwIfNotOk)
		.then(() => this.refetchEventTokens())
	}

	public deprecateToken(eventid: string) {
		fetch(`${PLATFORM}/event_tokens/${eventid}`, {
			method: "DELETE",
			...this.auth.configureFetch()
		})
		.then(throwIfNotOk)
		.then(() => this.refetchEventTokens())
	}

	public useEventSchedule(eventid: string): ScheduleEntryData[] | undefined {
		const [schedule, setSchedule] = useState<ScheduleEntryData[] | undefined>(undefined)
		useEffect(() => {
			fetch(`${PLATFORM}/event/${eventid}/schedule`)
				.then(throwIfNotOk)
				.then(r => r.json())
				.then(d => setSchedule(d))
		}, [eventid])
		return schedule
	}

	public useEventSubmissions(eventid: string): SubmissionData[] | undefined {
		const [submissions, setSubmissions] = useState<SubmissionData[] | undefined>(undefined)
		useEffect(() => {
			fetch(`${PLATFORM}/event/${eventid}/submissions`)
				.then(throwIfNotOk)
				.then(r => r.json())
				.then(d => setSubmissions(d))
		}, [eventid])
		return submissions
	}
}

function throwIfNotOk(r: Response): Response {
	if (!r.ok) {
		throw r
	}
	return r;
}
