"use client"

import { usePlatform } from "@/platform";
import { EventPageProps } from "../page";
import { use } from "react";
import Submission from "./submission";

export default function Home(props: EventPageProps) {
	const platform = usePlatform()
	const eventid = use(props.params).eventid
	const event = platform.useEvent(eventid);
	const schedule = platform.useEventSubmissions(eventid)
	
	if (schedule === undefined) {
		return <main><h1>Loading...</h1></main>
	}
	return <main>
		<h1>Submissions for {event?.name}</h1>
		<ul>
		{schedule.map(e =>
			<Submission key={e.id} eventId={eventid} data={e}></Submission>
		)}
		</ul>
	</main>
}
