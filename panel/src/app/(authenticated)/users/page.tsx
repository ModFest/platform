"use client"

import { usePlatform } from "@/platform";
import { UserData } from "@/platform_types";
import { use, useState } from "react";
import styles from "./page.module.css"
import Modal from 'react-modal';
import Form from 'next/form'

type SearchParams = Promise<{ [key: string]: string | string[] | undefined }>
type SearchParamProps = {
	searchParams: SearchParams;
};

export default function Home({ searchParams }: SearchParamProps) {
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
	if (u == undefined) {
		return <></>
	}

	const onSubmit = (data: FormData) => {
		platform.updateUser(JSON.parse(asString(data, "user"))).then(() => {
			setSuccess(true)
		}).catch(() => {
			setSuccess(false)
		})
	}

	return <>
		<h1>Editing {u.id}</h1>
		<Form action={onSubmit}>
			<textarea className={styles["textarea"]} name="user" defaultValue={JSON.stringify(u, undefined, 4)}>
			</textarea>
			<br></br>
			<button>Save</button>
		</Form>
		{success === undefined ? <></> : success ? "Updated succesfully" : "Something went wrong whilst updating"}
	</>
}

function UserCard(props: {user: UserData, forceExpand: boolean}) {
	const u = props.user
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
		<button onClick={() => setExpanded(!isExpanded)}>Expand</button><br></br>
		{userdata}
	</div>
}
