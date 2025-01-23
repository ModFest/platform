package net.modfest.botfest.extensions

import dev.kord.core.behavior.interaction.response.edit
import dev.kordex.core.components.forms.ModalForm
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.i18n.toKey
import dev.kordex.core.i18n.types.Key
import dev.kordex.core.i18n.withContext
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

			// We're using KordEx's unsafe api here, because our modal is optional and has prefilled fields.
			// This means we're responsible for initiating the response
			// Discords expects that, if we want to reply with a modal, the modal needs to be the first thing
			// we send. Afterward, we can reply with ephemeral messages.
			initialResponse = InitialSlashCommandResponse.None

			action {
				val curEvent = platform.getCurrentEvent().event;
				if (curEvent == null) {
					ackEphemeral {
						content = Translations.Commands.Register.Response.noevent
							.withContext(this@action)
							.translateNamed()
					}
					return@action // Do *not* try to send any modals, we've already replied
				}

				var platformUser = platform.getUser(this.user)

				var message = if (platformUser == null) {
					val modal = RegisterModal()

					modal.displayName.initialValue = this.member?.asMember()?.effectiveName?.toKey(Locale.UK)
					modal.displayName.translateInitialValue = false

					val result = modal.sendAndDeferEphemeral(this)

					// Result will be null if the user didn't enter anything
					// or if the modal timed out
					if (result == null) {
						return@action
					}

					platformUser = platform.authenticatedAsBotFest().createUser(UserCreateData(
						modal.displayName.value,
						modal.pronouns.value,
						modal.modrinthSlug.value,
						this.user.id.value.toString()
					))

					result
				} else {
					// User already has their data registered in platform
					// Ack immediately: we only have 5 seconds for any ack
					ackEphemeral()
				}

				// Okay, we now have a user that's registered. And we have a
				// message we can edit to respond to the user
				// let's perform the actual registration
				val event = platform.getEvent(curEvent)

				if (platformUser.registered.contains(curEvent)) {
					message.edit {
						content = Translations.Commands.Register.Response.already
							.withContext(this@action)
							.translateNamed(
								"event" to event.name
							)
					}
					return@action
				}

				platform.withAuth(user).registerMe(event)
				message.edit {
					content = Translations.Commands.Register.Response.success
						.withContext(this@action)
						.translateNamed(
							"event" to event.name
						)
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
