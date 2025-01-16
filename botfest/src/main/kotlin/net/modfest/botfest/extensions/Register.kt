package net.modfest.botfest.extensions

import dev.kordex.core.components.forms.ModalForm
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.i18n.toKey
import dev.kordex.core.i18n.types.Key
import dev.kordex.core.koin.KordExKoinComponent
import dev.kordex.modules.dev.unsafe.annotations.UnsafeAPI
import dev.kordex.modules.dev.unsafe.commands.slash.InitialSlashCommandResponse
import dev.kordex.modules.dev.unsafe.extensions.unsafeSlashCommand
import net.modfest.botfest.Platform
import net.modfest.botfest.i18n.Translations
import net.modfest.platform.pojo.UserCreateData
import org.koin.core.component.inject
import java.util.*

/**
 * Provides various debugging commands
 */
class Register : Extension(), KordExKoinComponent {
	override val name = "register"
	val platform: Platform by inject()

	@OptIn(UnsafeAPI::class)
	override suspend fun setup() {
		val platform = bot.getKoin().get<Platform>()

		unsafeSlashCommand {
			name = Translations.Commands.Register.name
			description = Translations.Commands.Register.description

			// We're manually responding with a modal, instead of doing it automatically
			initialResponse = InitialSlashCommandResponse.None

			action {
				val modal = RegisterModal()

				modal.displayName.initialValue = this.member?.asMember()?.effectiveName?.toKey(Locale.UK)
				modal.displayName.translateInitialValue = false

				val result = modal.sendAndDeferEphemeral(this)

				// Result will be null if the user didn't enter anything
				if (result != null) {
					platform.authenticatedAsBotFest().createUser(UserCreateData(
						modal.displayName.value,
						modal.pronouns.value,
						modal.modrinthSlug.value,
						this.user.id.value.toString()
					))
				}
			}
		}
	}

	class RegisterModal : ModalForm() {
		override var title: Key = Translations.Modal.Register.title

		val displayName = lineText {
			label = Translations.Modal.Register.Name.label
			placeholder = Translations.Modal.Register.Name.placeholder
			minLength = 2
			maxLength = 32
			required = true
		}

		val modrinthSlug = lineText {
			label = Translations.Modal.Register.Modrinthslug.label
			placeholder = Translations.Modal.Register.Modrinthslug.placeholder
			minLength = 1
			maxLength = 24
			required = true
		}

		val pronouns = lineText {
			label = Translations.Modal.Register.Pronouns.label
			placeholder = Translations.Modal.Register.Pronouns.placeholder
			minLength = 1
			maxLength = 24
			required = true
		}
	}
}
