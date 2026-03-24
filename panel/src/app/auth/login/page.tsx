import Inner from "./inner";

type SearchParams = Promise<{ [key: string]: string | string[] | undefined }>
type SearchParamProps = {
	searchParams: SearchParams;
};

export default async function Home(props: SearchParamProps) {
	// We obtain the page to redirect to after this whole ordeal.
	// If it's not specified we set it to ""
	let redirect = (await props.searchParams)["r"] ?? ""
	if (typeof(redirect) !== "string") {
		redirect = redirect[0]
	}
	return <Inner redirect_url={redirect}></Inner>
}
