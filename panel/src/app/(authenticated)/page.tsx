"use client"

import { usePlatform } from "@/platform";

export default function Home() {
	const platform = usePlatform()
	const users = platform.useAllUsers()

	return <>
		<h1>ModFest panel</h1>
		<ul>
			{
				users.map(u => <li>{u.name}</li>)
			}
		</ul>
	</>;
}
