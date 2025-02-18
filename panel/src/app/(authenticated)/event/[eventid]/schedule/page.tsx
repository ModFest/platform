"use client"

import { usePlatform } from "@/platform";
import { EventPageProps } from "../page";
import { use } from "react";

export default function Home(props: EventPageProps) {
	const platform = usePlatform()
	const eventid = use(props.params).eventid
	const schedule = platform.useEventSchedule(eventid)
	
	if (schedule === undefined) {
		return <main><h1>Loading...</h1></main>
	}
	// TODO proper name
	return <main>
		<h1>Schedule for {eventid}</h1>
		<ul>
		{schedule.map(e =>
			<li key={e.id}>
				{e.title}
			</li>
		)}
		</ul>
	</main>
}
