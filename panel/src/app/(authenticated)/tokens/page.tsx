"use client"

import { usePlatform } from "@/platform"
import styles from "./tokens.module.css"

export default function Page() {
	const platform = usePlatform()
	const events = platform.useAllEvents()
	const tokens = platform.useEventTokens()

	if (!events || !tokens) return <>Loading...</>

	const eventIds = distinct(events.map(e => e.id).concat(Object.keys(tokens.mc_server_tokens)))
	
	return <table>
		<tbody>
			{eventIds.map(id => 
				<tr key={id} className={styles["row"]}>
					<td>{id}</td>
					<td><pre style={{margin: 0}}>{tokens.mc_server_tokens[id] ?? ""}</pre></td>
					<td className={styles["regenCell"]}>
						<button onClick={() => platform.regenerateToken(id)}>
							Regenerate
						</button>
					</td>
					<td>
						{tokens.mc_server_tokens[id] !== undefined &&
							<button onClick={() => platform.deprecateToken(id)}>
								Delete
							</button>}
					</td>
					<td>
						{tokens.mc_server_tokens[id] !== undefined &&
							<button onClick={() => navigator.clipboard.writeText(tokens.mc_server_tokens[id])}>
								Copy to Clipboard
							</button>}
					</td>
				</tr>
			)}
		</tbody>
	</table>
}

function distinct<T>(iterable: Iterable<T> | null | undefined): T[] {
	return [...new Set(iterable)]
}
