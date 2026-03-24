import Link from "next/link";
import styles from "./sidebar.module.css"
import { useLogout } from "./template";
import Image from "next/image";
import { usePlatform } from "@/platform";
import { useEffect, useState } from "react";

export default function Sidebar() {
	let logout = useLogout();
	let platform = usePlatform();
	let currentEvent = platform.useCurrentEvent();
	let allEvents = platform.useAllEvents();
	let [selectedEvent, setSelectedEvent] = useState<string | undefined>();
	
	if (selectedEvent == undefined && currentEvent?.event) {
		setSelectedEvent(currentEvent.event);
	}

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
		<select className={styles["navelem"]} value={selectedEvent} onChange={e => setSelectedEvent(e.target.value)}>
			{allEvents?.map((event) => 
				<option key={event.id} value={event.id}>{event.name}</option>
			)}
		</select>
		<EventLink selectedEvent={selectedEvent} subpage="/submissions">Submissions</EventLink>
		<EventLink selectedEvent={selectedEvent} subpage="/schedule">Schedule</EventLink>
		<div className={styles["sidebar-vertical-padding"]}></div>
		<button className={styles["navelem"]} onClick={() => logout()}>Logout</button>
	</nav>
}

/**
 * A link to a page under /event/
 */
function EventLink(props: { selectedEvent: string | undefined, subpage: string, children: React.ReactNode}) {
	let style = `${styles["navelem"]} ${styles["subnavelem"]}`
	if (props.selectedEvent == undefined) {
		return <div className={`${style} ${styles["disabled"]}`} role="link" aria-disabled={true}>{props.children}</div>
	}
	return <Link 
		className={style}
		href={`/event/${props.selectedEvent}${props.subpage}`}
	>
		{props.children}
	</Link>
}
