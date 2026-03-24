import { usePlatform } from "@/platform";
import { SubmissionData } from "@/platform_types"
import Image from "next/image";
import styles from "./submission.module.css";

export type SubmissionProps = {
	data: SubmissionData,
	eventId: string,
}

export default function Submission(props: SubmissionProps) {
	const submission = props.data;
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
				By {submission.authors.map(a => <Author id={a}/>).reduce((a,b) => <>{a}, {b}</>)}
			</p>
			<p className={styles["description"]}>
				{submission.description}
			</p>
		</div>
	</div>
}

function Author(props: { id: string }) {
	const platform = usePlatform();
	const user = platform.useUser(props.id);
	if (!user) return <>Loading...</>

	return <b>{user.name}</b>
}
