import { Platform } from "@/platform";
import { SubmissionData } from "@/platform_types";

export async function getAllCredits(platform: Platform, submissions: SubmissionData[]) {
	let submissionSorted = submissions.toSorted((a,b) => a.name.localeCompare(b.name, "en-GB"))
	return {
		"discipline": "Showcase Teams",
		"titles": await Promise.all(submissionSorted.map(submission => getCredits(platform, submission)))
	}
}

export async function getCredits(platform: Platform, submission: SubmissionData) {
	let authors = await Promise.all(submission.authors.map(a => platform.getUser(a)));
	return {
		"title": submission.name,
		"names": authors.map(a => a.name)
	}
}
