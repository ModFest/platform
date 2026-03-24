"use client"

import { usePlatform } from "@/platform";
import { EventPageProps } from "../page";
import { use } from "react";
import Submission from "./submission";
import { getAllCredits } from "./credits";

export default function Home(props: EventPageProps) {
	const platform = usePlatform()
	const eventid = use(props.params).eventid
	const event = platform.useEvent(eventid);
	const submissions = platform.useEventSubmissions(eventid)
	
	if (submissions === undefined) {
		return <main><h1>Loading...</h1></main>
	}

	const copyAllCredits = () => {
		getAllCredits(platform, submissions).then(credits => {
			navigator.clipboard.writeText(JSON.stringify(credits, null, 4));
		})
	}

	return <main>
		<h1>Submissions for {event?.name}</h1>
		<button onClick={copyAllCredits}>Copy all credits</button>
		{submissions.map(e =>
			<Submission key={e.id} eventId={eventid} data={e}></Submission>
		)}
	</main>
}
