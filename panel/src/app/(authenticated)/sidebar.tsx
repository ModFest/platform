import Link from "next/link";
import styles from "./sidebar.module.css"
import { useLogout } from "./template";
import Image from "next/image";

export default function Sidebar() {
	let logout = useLogout();
	return <nav className={styles["sidebar"]}>
		<h1>
			<Image
				src="/logo-transparent-light.svg"
				alt="" // Image is decorative, shouldn't have alt
				width={100}
				height={100}
				className={styles["sidebar-logo"]}
			/>
			Panel
		</h1>
		<Link className={styles["navelem"]} href="/users">Users</Link>
		<Link className={styles["navelem"]} href="/tokens">Tokens</Link>
		<Link className={styles["navelem"]} href="/event">Event</Link>
		<div className={styles["sidebar-vertical-padding"]}></div>
		<button className={styles["navelem"]} onClick={() => logout()}>Logout</button>
	</nav>
}
