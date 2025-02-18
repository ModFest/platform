"use client"

import { usePlatform } from "@/platform";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

type SearchParams = Promise<{ [key: string]: string | string[] | undefined }>
type SearchParamProps = {
	searchParams: SearchParams;
};

export default function Home() {
	const router = useRouter()
	const platform = usePlatform()
	const [noCurrent, setNoCurrent] = useState(false)

	useEffect(() => {
		platform.getCurrentEvent().then(curEvent => {
			if (curEvent.event === null) {
				setNoCurrent(true)
			} else {
				router.push("/event/"+curEvent.event)
			}
		})
	}, [platform])

	return <main>
		{noCurrent ? <h1>Please select an event from the nav bar</h1> : <h1>Redirecting...</h1>}
	</main>
}
