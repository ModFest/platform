package template.extensions

import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.converters.impl.coalescingDefaultingString
import dev.kordex.core.commands.converters.impl.defaultingString
import dev.kordex.core.commands.converters.impl.user
import dev.kordex.core.components.components
import dev.kordex.core.components.publicButton
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.extensions.publicSlashCommand
import dev.kordex.core.i18n.withContext
import dev.kordex.core.koin.KordExKoinComponent
import template.MAIN_GUILD_ID
import template.Platform
import template.i18n.Translations


class TestExtension : Extension(), KordExKoinComponent {
//	val platform: Platform by inject()
	override val name = "test"


	override suspend fun setup() {
		var platform = bot.getKoin().get<Platform>()

		ephemeralSlashCommand {
			name = Translations.Commands.Health.name
			description = Translations.Commands.Health.description

			guild(MAIN_GUILD_ID)  // Otherwise it will take up to an hour to update

			action {
				val version = platform.getVersion()
				respond {
					content = Translations.Commands.Health.response
						.withContext(this@action)
						.translateNamed(
							"version" to version,
						)
				}
			}
		}
	}

	inner class SlapSlashArgs : Arguments() {
		val target by user {
			name = Translations.Arguments.Target.name
			description = Translations.Arguments.Target.description
		}

		// Slash commands don't support coalescing strings
		val weapon by defaultingString {
			name = Translations.Arguments.Weapon.name

			defaultValue = "🐟"
			description = Translations.Arguments.Weapon.description
		}
	}
}
