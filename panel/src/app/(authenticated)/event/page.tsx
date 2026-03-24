"use client"

import { usePlatform } from "@/platform";
import { useRouter } from "next/navigation";

export default function Home() {
	const router = useRouter()
	const platform = usePlatform()
	const currentEvent = platform.useCurrentEvent();
	
	const noCurrent = currentEvent && currentEvent.event == null;

	if (currentEvent && currentEvent.event) {
		router.push("/event/"+currentEvent.event);
	}

	return <main>
		{noCurrent ? <h1>Please select an event from the nav bar</h1> : <h1>Redirecting...</h1>}
	</main>
}
