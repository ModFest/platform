package net.modfest.botfest

import dev.kordex.core.time.TimestampType
import java.util.*
import java.time.Instant

/**
 * Extension function that formats a java Date as a discord timestamp.
 * Eg, it formats the date as a <t:unix_seconds:FORMAT> string
 * @see https://discordtimestamp.com/
 */
fun Instant.format(type: TimestampType): String {
	return type.format(this.epochSecond)
}
