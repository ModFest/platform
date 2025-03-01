package net.modfest.botfest.extensions

import dev.kord.common.annotation.KordUnsafe
import dev.kord.core.behavior.interaction.ActionInteractionBehavior
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.entity.User
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.rest.builder.message.create.InteractionResponseCreateBuilder
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.EphemeralSlashCommand
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.application.slash.group
import dev.kordex.core.components.forms.ModalForm
import dev.kordex.core.events.EventContext
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.extensions.event
import dev.kordex.core.i18n.toKey
import dev.kordex.core.i18n.types.Key
import dev.kordex.core.i18n.withContext
import dev.kordex.core.koin.KordExKoinComponent
import dev.kordex.core.types.TranslatableContext
import dev.kordex.modules.dev.unsafe.annotations.UnsafeAPI
import dev.kordex.modules.dev.unsafe.commands.slash.InitialSlashCommandResponse
import dev.kordex.modules.dev.unsafe.commands.slash.UnsafeSlashCommand
import dev.kordex.modules.dev.unsafe.components.forms.UnsafeModalForm
import dev.kordex.modules.dev.unsafe.extensions.unsafeSlashCommand
import net.modfest.botfest.CommandReferences
import net.modfest.botfest.MAIN_GUILD_ID
import net.modfest.botfest.Platform
import net.modfest.botfest.i18n.Translations
import net.modfest.platform.pojo.SubmitRequestOther
import net.modfest.platform.pojo.UserCreateData
import org.koin.core.component.inject
import java.util.*
import java.util.regex.Pattern

/**
 * Provides various debugging commands
 */
@OptIn(UnsafeAPI::class)
class EventCommands : Extension(), KordExKoinComponent {
	val cmds: CommandReferences by inject()
	override val name = "event"
	val platform: Platform by inject()

	@OptIn(KordUnsafe::class)
	override suspend fun setup() {
		// Register command
		event<GuildButtonInteractionCreateEvent> {
			check {
				failIfNot(event.interaction.componentId == "modfest-registration-button")
			}
			action {
				doRegister(
					{ event.interaction.respondEphemeral(it) },
					{ event.interaction.deferEphemeralResponseUnsafe() },
					{ it.sendAndDeferEphemeral(this@action) },
					event.interaction.user
				)
			}
		}

		cmds.registerCommand = unsafeSlashCommand {
			name = Translations.Commands.Register.name
			description = Translations.Commands.Register.description
			guild(MAIN_GUILD_ID)

			// We're using KordEx's unsafe api here, because our modal is optional and has prefilled fields.
			// This means we're responsible for initiating the response
			// Discords expects that, if we want to reply with a modal, the modal needs to be the first thing
			// we send. Afterward, we can reply with ephemeral messages.
			initialResponse = InitialSlashCommandResponse.None

			action {
				doRegister(
					{ ackEphemeral(it) },
					{ ackEphemeral() },
					{ it.sendAndDeferEphemeral(this@action) },
					this.user.asUser()
				)
			}
		}

		// event subcommands
		ephemeralSlashCommand {
			name = Translations.Commands.Group.Event.name
			description = Translations.Commands.Group.Event.description

			guild(MAIN_GUILD_ID)

			// unregister command
			cmds.unregisterCommand = ephemeralSubCommand {
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
		}
	}

	suspend inline fun TranslatableContext.doRegister(respond: (InteractionResponseCreateBuilder.() -> Unit) -> EphemeralMessageInteractionResponseBehavior, ackBlank: () -> EphemeralMessageInteractionResponseBehavior, sendModal: (ModalForm) -> EphemeralMessageInteractionResponseBehavior?, user: User) {
		val curEvent = platform.getCurrentEvent().event;
		if (curEvent == null) {
			val c = Translations.Commands.Register.Response.noevent
				.withContext(this@doRegister)
				.translateNamed()
			respond {
				content = c
			}
			return // Do *not* try to send any modals, we've already replied
		}

		var platformUser = platform.getUser(user)

		val message = if (platformUser == null) {
			val modal = RegisterModal()

			modal.displayName.initialValue = user.asMember(MAIN_GUILD_ID).effectiveName.toKey(Locale.UK)
			modal.displayName.translateInitialValue = false

			val result = sendModal(modal)

			// Result will be null if the user didn't enter anything
			// or if the modal timed out
			if (result == null) {
				return
			}

			platformUser = platform.authenticatedAsBotFest().createUser(UserCreateData(
				modal.displayName.value,
				modal.pronouns.value,
				modal.modrinthSlug.value,
				user.id.value.toString()
			))

			result
		} else {
			// User already has their data registered in platform
			// Ack immediately: we only have 5 seconds for any ack
			ackBlank()
		}

		// Okay, we now have a user that's registered. And we have a
		// message we can edit to respond to the user
		// let's perform the actual registration
		val event = platform.getEvent(curEvent)

		if (platformUser.registered.contains(curEvent)) {
			message.edit {
				content = Translations.Commands.Register.Response.already
					.withContext(this@doRegister)
					.translateNamed(
						"event" to event.name
					)
			}
			return
		}

		platform.withAuth(user).registerMe(event)
		message.edit {
			content = Translations.Commands.Register.Response.success
				.withContext(this@doRegister)
				.translateNamed(
					"event" to event.name
				)
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
