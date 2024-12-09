package net.modfest.botfest.extensions

import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.converters.impl.optionalString
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.i18n.withContext
import dev.kordex.core.koin.KordExKoinComponent
import dev.kordex.core.utils.suggestStringCollection
import net.modfest.botfest.MAIN_GUILD_ID
import net.modfest.botfest.Platform
import net.modfest.botfest.i18n.Translations
import net.modfest.platform.pojo.CurrentEventData
import org.koin.core.component.inject

/**
 * Provides privileged commands that should be available only to admins
 */
class AdminCommands : Extension(), KordExKoinComponent {
	override val name = "admin"
	val platform: Platform by inject()

	override suspend fun setup() {
		ephemeralSlashCommand {
			name = Translations.Commands.Group.Admin.name
			description = Translations.Commands.Group.Admin.description

			guild(MAIN_GUILD_ID)  // Otherwise it will take up to an hour to update

			// Set the current event
			ephemeralSubCommand(::SetCurrentEventArgs) {
				name = Translations.Commands.Setcurrentevent.name
				description = Translations.Commands.Setcurrentevent.description

				action {
					platform.setCurrentEvent(
						CurrentEventData(arguments.event)
					)

					// Refetch just to be sure
					var curEvent = platform.getCurrentEvent()

					respond {
						content = Translations.Commands.Setcurrentevent.response
							.withContext(this@action)
							.translateNamed(
								"event" to curEvent.event
							)
					}
				}
			}

			// Reload files from filesystem / invalidate the in-memory cache
			ephemeralSubCommand {
				name = Translations.Commands.Reload.name
				description = Translations.Commands.Reload.description

				action {
					val res = platform.reloadFromFilesystem()

					respond {
						content = Translations.Commands.Reload.response
							.withContext(this@action)
							.translateNamed(
								"numStores" to res
							)
					}
				}
			}
		}
	}

	inner class SetCurrentEventArgs : Arguments() {
		val event by optionalString {
			name = Translations.Arguments.Event.name
			description = Translations.Arguments.Event.description

			autoComplete {
				suggestStringCollection(platform.getEventIds())
			}
		}
	}
}
