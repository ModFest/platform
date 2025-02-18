"use client"
import { usePlatform } from "@/platform"
import { use } from "react"

export type EventPageProps = {
	// NextJs gives us this promise which contains the [eventid] part of the url
	params: Promise<{ eventid: string }>
}

export default function Home(props: EventPageProps) {
	const eventid = use(props.params).eventid
	const platform = usePlatform()
	const e = platform.useEvent(eventid)
	return <main>
		<h1>Event info about {e?.name}</h1>
	</main>
}
