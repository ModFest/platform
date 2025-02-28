package net.modfest.botfest.extensions

import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.createMessage
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.application.slash.publicSubCommand
import dev.kordex.core.commands.converters.impl.attachment
import dev.kordex.core.commands.converters.impl.optionalString
import dev.kordex.core.components.components
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.i18n.withContext
import dev.kordex.core.koin.KordExKoinComponent
import dev.kordex.core.utils.suggestStringCollection
import dev.kordex.modules.dev.unsafe.annotations.UnsafeAPI
import net.modfest.botfest.CommandReferences
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
	val commands: CommandReferences by inject()

	@OptIn(UnsafeAPI::class)
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
					val res = platform.withAuth(this.user).reloadFromFilesystem()

					respond {
						content = Translations.Commands.Reload.response
							.withContext(this@action)
							.translateNamed(
								"numStores" to res
							)
					}
				}
			}

			// Sends the message where users can press a button to update
			ephemeralSubCommand {
				name = Translations.Commands.Admin.Registermsg.name
				description = Translations.Commands.Admin.Registermsg.description

				action {
					val curEvent = platform.getCurrentEvent().event!!
					val eventData = platform.getEvent(curEvent)
					this.channel.createMessage {
						embed {
							title = Translations.Commands.Admin.Registermsg.Embed.title
								.translateNamed(
									"event_name" to eventData.name
								)
							description = Translations.Commands.Admin.Registermsg.Embed.content
								.translateNamed(
									"event_name" to eventData.name,
									"cmdRegister" to (commands.registerCommand?.mention ?: "/register"),
									"cmdUnregister" to (commands.unregisterCommand?.mention ?: "/event unregister"),
								)
							color = dev.kord.common.Color(Integer.parseInt(eventData.colors.primary, 16))
						}
						actionRow {
							interactionButton(ButtonStyle.Primary, "modfest-registration-button") {
								label = Translations.Commands.Admin.Registermsg.button
									.translateNamed()
							}
						}
					}
					respond {
						content = "Done"
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
