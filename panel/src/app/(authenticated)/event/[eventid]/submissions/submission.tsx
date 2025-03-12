import { usePlatform } from "@/platform";
import { SubmissionData } from "@/platform_types"

export type SubmissionProps = {
	data: SubmissionData,
	eventId: string,
}

export default function Submission(props: SubmissionProps) {
	const submission = props.data;
	return <>
		<h2>{submission.name}</h2>
		<ul>
			{submission.authors.map(a => <li key={a}>
				<Author id={a} eventId={props.eventId} submissionId={submission.id}></Author>
				</li>)}
		</ul>
	</>
}

function Author(props: { id: string, eventId: string, submissionId: string }) {
	const platform = usePlatform();
	const user = platform.useUser(props.id);
	if (!user) return <>Loading...</>

	const doDelete = () => {
		platform.removeAuthorFromSubmission(props.eventId, props.submissionId, props.id);
	};
	return <>{user.name} <button onClick={doDelete}>X</button></>
}
