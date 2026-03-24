import Link from "next/link";
import styles from "./sidebar.module.css"
import globalStyles from "@/globalstyles.module.css";
import { useLogout } from "./template";
import Image from "next/image";
import { usePlatform } from "@/platform";
import { useState } from "react";
import { usePathname } from "next/navigation";

export default function Sidebar() {
	const pathname = usePathname();
	const logout = useLogout();
	const platform = usePlatform();
	const currentEvent = platform.useCurrentEvent();
	const allEvents = platform.useAllEvents();
	const [selectedEvent, setSelectedEvent] = useState<string | undefined>();
	
	if (selectedEvent == undefined) {
		// If we're on an event page, the event-select dropdown will default to that event
		// Otherwise, it'll default to the currently running event
		const eventMatch = pathname.match(/\/event\/([^/]+)/);
		if (eventMatch) {
			setSelectedEvent(eventMatch[1]);
		} else if (currentEvent?.event) {
			setSelectedEvent(currentEvent.event);
		}
	}

	const onSelectedEventChange = (newEvent: string) => {
		setSelectedEvent(newEvent);
		// If we're currently on an event page, we redirect to the one that was selected
		if (window.location.pathname.startsWith("/event")) {
			window.location.pathname = window.location.pathname.replace(/\/event\/[^/]+/, `/event/${newEvent}`);
		}
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
		<select className={styles["navelem"]} value={selectedEvent} onChange={e => onSelectedEventChange(e.target.value)}>
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
	const style = `${styles["navelem"]} ${styles["subnavelem"]}`
	if (props.selectedEvent == undefined) {
		return <div className={`${style} ${globalStyles["disabled"]}`} role="link" aria-disabled={true}>{props.children}</div>
	}
	return <Link 
		className={style}
		href={`/event/${props.selectedEvent}${props.subpage}`}
	>
		{props.children}
	</Link>
}
