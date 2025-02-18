"use client"

import { getRedirectUrl } from "../auth";

type SearchParams = Promise<{ [key: string]: string | string[] | undefined }>
type SearchParamProps = {
	searchParams: SearchParams;
};

export default function Home(props: SearchParamProps) {
	const modrinthSite = process.env.NEXT_PUBLIC_MODRINTH_SITE
	const clientId = process.env.NEXT_PUBLIC_MODRINTH_APP_ID!
	const callback = getRedirectUrl()
	const scope = `USER_READ`

  	const oathUrl = `${modrinthSite}/auth/authorize?client_id=${encodeURIComponent(clientId)}&redirect_uri=${encodeURIComponent(callback)}&scope=${encodeURIComponent(scope)}`

	return (
		<main>
			<center>
				<h1>ModFest panel</h1>
				<a href={oathUrl}>Log in with Modrinth</a>
			</center>
		</main>
	);
}
