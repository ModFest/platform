import { SubmissionData } from "@/platform_types"

export type SubmissionProps = {
	data: SubmissionData
}

export default function Submission(props: SubmissionProps) {
	let submission = props.data;
	return <>
		<h2>{submission.name}</h2>
	</>
}
