package net.modfest.botfest.extensions

import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.i18n.withContext
import dev.kordex.core.koin.KordExKoinComponent
import dev.kordex.core.time.TimestampType
import net.modfest.botfest.MAIN_GUILD_ID
import net.modfest.botfest.Platform
import net.modfest.botfest.format
import net.modfest.botfest.i18n.Translations
import java.time.Instant
import java.util.*

/**
 * Provides various debugging commands
 */
class DebugCommands : Extension(), KordExKoinComponent {
	override val name = "debug"
	// Store the moment that this extension was created, so we can tell approximately when the bot was started
	val startupTime = Date.from(Instant.now())

	override suspend fun setup() {
		val platform = bot.getKoin().get<Platform>()

		ephemeralSlashCommand {
			name = Translations.Commands.Group.Debug.name
			description = Translations.Commands.Group.Debug.description

			guild(MAIN_GUILD_ID)  // Otherwise it will take up to an hour to update

			// View health of the bot and platform
			ephemeralSubCommand {
				name = Translations.Commands.Health.name
				description = Translations.Commands.Health.description

				action {
					val botHealth = Translations.Commands.Health.Response.bot
						.withContext(this@action)
						.translateNamed(
							"startupTime" to startupTime.format(TimestampType.RelativeTime)
						)

					var platformHealth: String;
					try {
						val platformHealthInfo = platform.getHealth()
						platformHealth = Translations.Commands.Health.Response.platform
							.withContext(this@action)
							.translateNamed(
								"health" to platformHealthInfo.health,
								"startupTime" to platformHealthInfo.runningSince.format(TimestampType.RelativeTime)
							)
					} catch (e: Throwable) {
						platformHealth = Translations.Commands.Health.Response.platformError
							.withContext(this@action)
							.translateNamed(
								"error" to "${e.javaClass.name}: ${e.message}",
							)
					}

					respond {
						content = botHealth + "\n" + platformHealth
					}
				}
			}

			// Get current event
			ephemeralSubCommand {
				name = Translations.Commands.Getcurrentevent.name
				description = Translations.Commands.Getcurrentevent.description

				action {
					val data = platform.getCurrentEvent()

					respond {
						content = Translations.Commands.Getcurrentevent.response
							.withContext(this@action)
							.translateNamed(
								"event" to data.event
							)
					}
				}
			}

			// Whoami command
			ephemeralSubCommand {
				name = Translations.Commands.Whoami.name
				description = Translations.Commands.Whoami.description

				action {
					val data = platform.withAuth(this.user).getAuthenticatedUserInfo()

					respond {
						content = Translations.Commands.Whoami.response
							.withContext(this@action)
							.translateNamed(
								"auth" to data.isAuthenticated,
								"id" to data.userId,
								"name" to data.name,
								"permissions" to (data.permissions?.joinToString("\n"))
							)
					}
				}
			}
		}
	}
}
