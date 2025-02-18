"use client"

import Link from "next/link"
import styles from "../sidebar.module.css"
import { useLogout } from "../template"

export default function Template({ children }: { children: React.ReactNode }) {
	const logout = useLogout()

	return <div>
		<nav className={styles["sidebar"]}>
			<h1>ModFest Panel</h1>
			<Link className={styles["navelem"]} href="/users">Users</Link>
			<Link className={styles["navelem"]} href="/event">Event</Link>
			<div className={styles["sidebar-vertical-padding"]}></div>
			<button className={styles["navelem"]} onClick={() => logout()}>Logout</button>
		</nav>
		<div className={styles["content"]}>
			{children}
		</div>
	</div>
}
