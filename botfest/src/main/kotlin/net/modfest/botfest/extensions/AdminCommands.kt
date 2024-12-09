package net.modfest.botfest.extensions

import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.converters.impl.enumChoice
import dev.kordex.core.commands.application.slash.converters.impl.stringChoice
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.converters.impl.defaultingString
import dev.kordex.core.commands.converters.impl.string
import dev.kordex.core.commands.converters.impl.stringList
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.i18n.toKey
import dev.kordex.core.i18n.types.Key
import dev.kordex.core.i18n.withContext
import dev.kordex.core.koin.KordExKoinComponent
import dev.kordex.core.time.TimestampType
import dev.kordex.core.utils.suggestStringCollection
import net.modfest.botfest.MAIN_GUILD_ID
import net.modfest.botfest.Platform
import net.modfest.botfest.format
import net.modfest.botfest.i18n.Translations
import java.time.Instant
import java.util.ArrayList
import java.util.Date

/**
 * Provides privileged commands that should be available only to admins
 */
class AdminCommands : Extension(), KordExKoinComponent {
	override val name = "admin"

	override suspend fun setup() {
		val platform = bot.getKoin().get<Platform>()

		ephemeralSlashCommand {
			name = Translations.Commands.Group.Admin.name
			description = Translations.Commands.Group.Admin.description

			guild(MAIN_GUILD_ID)  // Otherwise it will take up to an hour to update

			// Set the current event
			ephemeralSubCommand(::SetCurrentEventArgs) {
				name = Translations.Commands.Setcurrentevent.name
				description = Translations.Commands.Setcurrentevent.description

				action {
					// TODO allow bot to set current event
				}
			}
		}
	}

	inner class SetCurrentEventArgs : Arguments() {
		val event by string {
			name = Translations.Arguments.Event.name
			description = Translations.Arguments.Event.description

			autoComplete {
				suggestStringCollection(arrayOf("A", "B").toList())
			}
		}
	}
}
