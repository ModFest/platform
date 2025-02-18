package net.modfest.botfest.extensions

import dev.kord.core.behavior.interaction.response.edit
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
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
import net.modfest.botfest.MAIN_GUILD_ID
import net.modfest.botfest.Platform
import net.modfest.botfest.i18n.Translations
import net.modfest.platform.pojo.UserCreateData
import org.koin.core.component.inject
import java.util.*
import java.util.regex.Pattern

/**
 * Provides various debugging commands
 */
class EventCommands : Extension(), KordExKoinComponent {
	override val name = "event"
	val platform: Platform by inject()

	var MODRINTH_REGEX = Pattern.compile(".*modrinth\\.com/mod/([\\w!@$()`.+,\"\\-']{3,64})/?.*");

	@OptIn(UnsafeAPI::class)
	override suspend fun setup() {
		val platform = bot.getKoin().get<Platform>()

		// Register command
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

		// event subcommands
		ephemeralSlashCommand {
			name = Translations.Commands.Group.Event.name
			description = Translations.Commands.Group.Event.description

			guild(MAIN_GUILD_ID)

			// unregister command
			ephemeralSubCommand {
				name = Translations.Commands.Event.Unregister.name
				description = Translations.Commands.Event.Unregister.description

				action {
					var curEvent = platform.getCurrentEvent().event;

					if (curEvent == null) {
						respond {
							content = Translations.Commands.Event.Unregister.Response.unavailable
								.withContext(this@action)
								.translateNamed()
						}
						return@action
					}

					val eventData = platform.getEvent(curEvent)
					val platformUser = platform.getUser(user)
					if (platformUser == null || !platformUser.registered.contains(curEvent)) {
						respond {
							content = Translations.Commands.Event.Unregister.Response.none
								.withContext(this@action)
								.translateNamed(
									"event" to eventData.name
								)
						}
						return@action
					}

					platform.withAuth(user).unregisterMe(eventData)
					respond {
						content = Translations.Commands.Event.Unregister.Response.success
							.withContext(this@action)
							.translateNamed(
								"event" to eventData.name
							)
					}
				}
			}

			// command for submitting a mod
			ephemeralSubCommand(::SubmitModal) {
				name = Translations.Commands.Event.Submit.name
				description = Translations.Commands.Event.Submit.description

				action { modal ->
					if (modal == null) return@action
					val curEvent = platform.getCurrentEvent().event

					if (curEvent == null) {
						respond {
							content = Translations.Commands.Event.Submit.Response.unavailable
								.withContext(this@action)
								.translateNamed()
						}
						return@action
					}

					val matcher = MODRINTH_REGEX.matcher(modal.modrinthUrl.value!!)

					if (!matcher.find()) {
						respond {
							content = Translations.Commands.Event.Submit.Response.invalid
								.withContext(this@action)
								.translateNamed(
									"url" to modal.modrinthUrl.value
								)
						}
						return@action
					}

					val projectSlug = matcher.group(1)

					val eventInfo = platform.getEvent(curEvent)
					val submission = platform.withAuth(this.user).submitModrinth(curEvent, projectSlug)

					respond {
						content = Translations.Commands.Event.Submit.Response.success
							.withContext(this@action)
							.translateNamed(
								"event" to eventInfo.name,
								"mod" to submission.name
							)
					}
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



	class SubmitModal : ModalForm() {
		override var title: Key = Translations.Modal.Submit.title

		val modrinthUrl = lineText {
			label = Translations.Modal.Submit.Url.label
			placeholder = Translations.Modal.Submit.Url.placeholder
			minLength = 10
			maxLength = 128
			required = true
		}
	}
}
