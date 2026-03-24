import { usePlatform } from "@/platform";
import { SubmissionData, UserData } from "@/platform_types"
import Image from "next/image";
import styles from "./submission.module.css";
import globalStyles from "@/globalstyles.module.css";
import { ReactNode } from "react";

export type SubmissionProps = {
	data: SubmissionData,
	eventId: string,
}

export default function Submission(props: SubmissionProps) {
	const submission = props.data;
	const platform = usePlatform();
	const [authorsIsPartial, authors] = platform.useUsers(props.data.authors); 

	const copyPings = () => {
		if (authorsIsPartial) return; // Shouldn't happen, the button should be disabled if we don't have author data yet
		const pings = authors.map(a => `<@${a.discord_id}>`);
		navigator.clipboard.writeText(pings.join(", "));
	}

	// Check if platform refused to provide a discord id for someone, this indicates a permission issue
	const discordPermIssues = authors.find(a => a && !a.discord_id) != undefined;
	const copyPingsDisabled = authorsIsPartial || discordPermIssues;

	return <div className={styles["card"]}>
		<div className={styles["icon"]}>
			{submission.images.icon &&
				<Image
					src={submission.images.icon}
					alt="" // Probably best to consider this icon as being decorative, since the title is already listed
					unoptimized
					width={100} height={100}/>}
		</div>
		<div className={styles["info"]}>
			<h2>{submission.name}</h2>
			<p>
				By {joinReactElements(authors.map(a => <Author userData={a}/>), ", ")}
			</p>
			<p className={styles["description"]}>
				{submission.description}
			</p>
			<button
				disabled={copyPingsDisabled}
				className={copyPingsDisabled ? globalStyles["disabled"] : ""}
				onClick={copyPings}
			>
				{discordPermIssues ? "Copy pings (no permissions)" : "Copy pings"}
			</button>
		</div>
	</div>
}

function joinReactElements(elements: ReactNode[], joiner: ReactNode) {
	if (elements.length == 0) return elements;
	return elements.reduce((a,b) => <>{a}{joiner}{b}</>);
}

function Author(props: { userData: UserData | undefined }) {
	const user = props.userData;
	if (!user) return <>Loading...</>

	return <b>{user.name}</b>
}
