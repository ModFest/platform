"use client"

import { usePlatform } from "@/platform";
import { UserData } from "@/platform_types";
import { use, useState } from "react";
import styles from "./page.module.css"
import Modal from 'react-modal';
import Form from 'next/form'
import { usePathname, useRouter } from "next/navigation";
import Link from "next/link";
import { Editor } from "@monaco-editor/react";

type SearchParams = Promise<{ [key: string]: string | string[] | undefined }>
type SearchParamProps = {
	searchParams: SearchParams;
};

export default function Home({ searchParams }: SearchParamProps) {
	const router = useRouter()
	const platform = usePlatform()
	const users = platform.useAllUsers()
	const [forceExpand, setForceExpand] = useState(false)

	const edit = use(searchParams)["edit"]

	const page = <>
		<h1>ModFest panel</h1>
		<button onClick={() => setForceExpand(!forceExpand)}>Expand all</button>
		{
			users.map(u => <UserCard key={u.id} forceExpand={forceExpand} user={u}></UserCard>)
		}
	</>
	const modal = <Modal
		isOpen={edit !== undefined}
		onRequestClose={() => {
			router.back()
		}}
		appElement={document.getElementsByTagName("body") || undefined}
		>
		<UserEdit user={users.find(u => u.id == edit)!}></UserEdit>
	</Modal>

	return <>
		{modal}
		{page}
	</>;
}

function asString(d: FormData, key: string): string {
	const v = d.get(key)!
	if (typeof v !== "string") {
		throw `${key} was weird type`
	}
	return v
}

function UserEdit(props: {user: UserData | undefined}) {
	const platform = usePlatform()
	const [success, setSuccess] = useState<boolean | undefined>(undefined)
	const u = props.user
	const [editorContent, setEditorContent] = useState("")

	if (u == undefined) {
		return <></>
	}

	const onSubmit = () => {
		platform.updateUser(JSON.parse(editorContent)).then(() => {
			setSuccess(true)
		}).catch(() => {
			setSuccess(false)
		})
	}

	return <>
		<h1>Editing {u.id}</h1>
		<div className={styles["textarea"]}>
			<Editor height="100%" defaultValue={JSON.stringify(u, undefined, 4)} language="json" onChange={(e) => {if (e) { setEditorContent(e) }}}>
			</Editor>
		</div>
		<br></br>
		<button onClick={onSubmit}>Save</button>
		{success === undefined ? <></> : success ? "Updated succesfully" : "Something went wrong whilst updating"}
	</>
}

function UserCard(props: {user: UserData, forceExpand: boolean}) {
	const u = props.user
	const pathname = usePathname();
	const [isExpanded, setExpanded] = useState(false)

	const userdata = (isExpanded || props.forceExpand) ?
		<table>
			<tbody>
				<tr>
					<th>Name</th>
					<td>{u.name}</td>
				</tr>
				<tr>
					<th>Pronouns</th>
					<td>{u.pronouns}</td>
				</tr>
				<tr>
					<th>Bio</th>
					<td>{u.bio}</td>
				</tr>
				<tr>
					<th>Discord</th>
					<td>{u.discord_id}</td>
				</tr>
				<tr>
					<th>Modrinth</th>
					<td>{u.modrinth_id}</td>
				</tr>
				<tr>
					<th>Icon</th>
					<td>{u.icon}</td>
				</tr>
				<tr>
					<th>Badges</th>
					<td>{u.badges?.join(", ")}</td>
				</tr>
				<tr>
					<th>Registered</th>
					<td>{u.registered?.join(", ")}</td>
				</tr>
				<tr>
					<th>Role</th>
					<td>{u.role}</td>
				</tr>
			</tbody>
		</table> : 
		<>Discord: {u.discord_id}, Modrinth: {u.modrinth_id}</>

	return <div className={styles["user-card"]}>
		<h2>{u.name}</h2> - {u.id}
		<button onClick={() => setExpanded(!isExpanded)}>Expand</button>
		<Link href={pathname+"?edit="+u.id}>Edit</Link><br></br>
		{userdata}
	</div>
}
