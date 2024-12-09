package template.extensions

import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.i18n.withContext
import dev.kordex.core.koin.KordExKoinComponent
import dev.kordex.core.time.TimestampType
import template.MAIN_GUILD_ID
import template.Platform
import template.format
import template.i18n.Translations

class TestExtension : Extension(), KordExKoinComponent {
	override val name = "test"

	override suspend fun setup() {
		val platform = bot.getKoin().get<Platform>()

		ephemeralSlashCommand {
			name = Translations.Commands.Group.Debug.name
			description = Translations.Commands.Group.Debug.description

			guild(MAIN_GUILD_ID)  // Otherwise it will take up to an hour to update

			ephemeralSubCommand {
				name = Translations.Commands.Health.name
				description = Translations.Commands.Health.description

				action {
					val health = platform.getHealth()

					respond {
						content = Translations.Commands.Health.response
							.withContext(this@action)
							.translateNamed(
								"health" to health.health,
								"startupTime" to health.runningSince.format(TimestampType.RelativeTime)
							)
					}
				}
			}
		}
	}
}
