import { usePlatform } from "@/platform";
import { SubmissionData } from "@/platform_types"

export type SubmissionProps = {
	data: SubmissionData
}

export default function Submission(props: SubmissionProps) {
	const submission = props.data;
	return <>
		<h2>{submission.name}</h2>
		<ul>
			{submission.authors.map(a => <li><Author key={a} id={a}></Author></li>)}
		</ul>
	</>
}

function Author(props: { id: string }) {
	const platform = usePlatform();
	const user = platform.useUser(props.id);
	if (!user) return <>Loading...</>
	return <>{user.name}</>
}
