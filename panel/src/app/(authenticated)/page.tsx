"use client"

import { usePlatform } from "@/platform";
import Link from "next/link";

export default function Home() {
	return <>
		<h1>ModFest panel</h1>
		<div>ZZZZZZZ: {process.env.NEXT_PUBLIC_TEST}</div>
		<Link href="./users">View users</Link>
	</>;
}
