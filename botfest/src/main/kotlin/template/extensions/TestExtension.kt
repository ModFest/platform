package template.extensions

import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.converters.impl.defaultingString
import dev.kordex.core.commands.converters.impl.user
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
		var platform = bot.getKoin().get<Platform>()

		ephemeralSlashCommand {
			name = Translations.Commands.Health.name
			description = Translations.Commands.Health.description

			guild(MAIN_GUILD_ID)  // Otherwise it will take up to an hour to update

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
