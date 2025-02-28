package net.modfest.botfest.extensions

import dev.kord.core.behavior.interaction.response.edit
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.converters.impl.stringChoice
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.application.slash.group
import dev.kordex.core.commands.converters.impl.string
import dev.kordex.core.components.forms.ModalForm
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.i18n.toKey
import dev.kordex.core.i18n.types.Key
import dev.kordex.core.i18n.withContext
import dev.kordex.core.koin.KordExKoinComponent
import dev.kordex.core.utils.suggestStringCollection
import dev.kordex.modules.dev.unsafe.annotations.UnsafeAPI
import dev.kordex.modules.dev.unsafe.commands.slash.InitialSlashCommandResponse
import dev.kordex.modules.dev.unsafe.extensions.unsafeSubCommand
import net.modfest.botfest.MAIN_GUILD_ID
import net.modfest.botfest.Platform
import net.modfest.botfest.i18n.Translations
import net.modfest.platform.pojo.SubmissionData.AssociatedData.Other
import net.modfest.platform.pojo.SubmissionPatchData
import net.modfest.platform.pojo.SubmitRequestOther
import org.koin.core.component.inject
import java.util.*
import java.util.regex.Pattern

/**
 * Provides various debugging commands
 */
class SubmissionCommands : Extension(), KordExKoinComponent {
	override val name = "submission"
	val platform: Platform by inject()

	var MODRINTH_REGEX = Pattern.compile(".*modrinth\\.com/mod/([\\w!@$()`.+,\"\\-']{3,64})/?.*");

	@OptIn(UnsafeAPI::class)
	override suspend fun setup() {
		// Commands for submitting
		ephemeralSlashCommand {
			name = Translations.Commands.Event.Submit.name
			description = Translations.Commands.Event.Submit.description

			guild(MAIN_GUILD_ID)

			// Submitting a modrinth project
			ephemeralSubCommand(::SubmitModalModrinth) {
				name = Translations.Commands.Event.Submit.Modrinth.name
				description = Translations.Commands.Event.Submit.Modrinth.description

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

			// Submitting a non-modrinth project
			ephemeralSubCommand(::SubmitModalOther) {
				name = Translations.Commands.Event.Submit.Other.name
				description = Translations.Commands.Event.Submit.Other.description

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

					val submission = platform.withAuth(this.user).submitOther(curEvent, SubmitRequestOther(
						modal.name.value!!,
						modal.description.value!!,
						setOf("dc:"+user.id),
						modal.homepage.value.convertBlankToNull(),
						modal.sourcecode.value.convertBlankToNull(),
						modal.downloadUrl.value.convertBlankToNull()
					)
					)
					val eventInfo = platform.getEvent(curEvent)

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

		ephemeralSlashCommand {
			name = Translations.Commands.Group.Submission.name
			description = Translations.Commands.Group.Submission.description

			guild(MAIN_GUILD_ID)

			unsafeSubCommand(::SubmissionArg) {
				name = Translations.Commands.Submission.Edit.name
				description = Translations.Commands.Submission.Edit.description

				initialResponse = InitialSlashCommandResponse.None

				action {
					val subId = this.arguments.submission
					val curEvent = platform.getCurrentEvent().event
					if (curEvent == null) {
						ackEphemeral {
							content = Translations.Commands.Event.Submit.Response.unavailable
								.withContext(this@action)
								.translateNamed()
						}
						return@action
					}

					val submission = platform.getUserSubmissions(this.user.id).find { it.id == subId }

					if (submission == null) {
						ackEphemeral {
							content = Translations.Commands.Submission.Edit.Response.notfound
								.withContext(this@action)
								.translateNamed(
									"subId" to subId
								)
						}
						return@action
					}

					val subPlatformData = submission.platform.inner
					val modal: SubmissionEditForm
					if (subPlatformData is Other) {
						modal = SubmissionEditOtherForm()
						if (subPlatformData.homepageUrl != null) {
							modal.homepage.initialValue = subPlatformData.homepageUrl!!.toKey(Locale.UK)
						}
					} else {
						modal = SubmissionEditForm()
					}
					modal.name.initialValue = submission.name.toKey(Locale.UK)
					modal.description.initialValue = submission.description.toKey(Locale.UK)
					if (submission.source != null) {
						modal.source.initialValue = submission.source!!.toKey(Locale.UK)
					}

					val result = modal.sendAndDeferEphemeral(this)

					// Result will be null if the user didn't enter anything
					// or if the modal timed out
					if (result == null) {
						return@action
					}

					platform.withAuth(this.user).editSubmissionData(curEvent, subId, SubmissionPatchData(
						modal.name.value,
						modal.description.value,
						modal.source.value,
						if (modal is SubmissionEditOtherForm) { modal.homepage.value } else { null }
					))

					result.edit {
						content = Translations.Commands.Submission.Edit.Response.success
							.withContext(this@action)
							.translateNamed(
								"subId" to subId
							)
					}
				}
			}
		}
	}

	inner class SubmissionArg : Arguments() {
		val submission by string {
			name = Translations.Arguments.Submission.Edit.name
			description = Translations.Arguments.Submission.Edit.description

			autoComplete {
				val curEvent = platform.getCurrentEvent().event ?: return@autoComplete
				suggestStringCollection(
					platform.getUserSubmissions(this.user.id)
						.filter { it.event == curEvent }
						.map { it.id }
				)
			}
		}
	}

	open class SubmissionEditForm : ModalForm() {
		override var title: Key = Translations.Modal.Submission.Edit.title

		val name = lineText {
			label = Translations.Modal.Submission.Name.label
			placeholder = Translations.Modal.Submission.Name.placeholder
			minLength = 2
			maxLength = 128
			required = true
			translateInitialValue = false
		}

		val description = lineText {
			label = Translations.Modal.Submission.Description.label
			placeholder = Translations.Modal.Submission.Description.placeholder
			minLength = 1
			maxLength = 128
			required = true
			translateInitialValue = false
		}

		val source = lineText {
			label = Translations.Modal.Submission.Source.label
			placeholder = Translations.Modal.Submission.Source.placeholder
			minLength = 0
			maxLength = 128
			required = false
			translateInitialValue = false
		}
	}

	class SubmissionEditOtherForm : SubmissionEditForm() {
		val homepage = lineText {
			label = Translations.Modal.Submission.Homepage.label
			placeholder = Translations.Modal.Submission.Homepage.placeholder
			minLength = 1
			maxLength = 128
			required = true
			translateInitialValue = false
		}
	}

	class SubmitModalModrinth : ModalForm() {
		override var title: Key = Translations.Modal.Submit.title

		val modrinthUrl = lineText {
			label = Translations.Modal.Submit.Url.label
			placeholder = Translations.Modal.Submit.Url.placeholder
			minLength = 10
			maxLength = 128
			required = true
		}
	}

	class SubmitModalOther : ModalForm() {
		override var title: Key = Translations.Modal.Submit.title

		val name = lineText {
			label = Translations.Modal.Submission.Name.label
			placeholder = Translations.Modal.Submission.Name.placeholder
			minLength = 1
			maxLength = 128
			required = true
		}

		val description = lineText {
			label = Translations.Modal.Submission.Description.label
			placeholder = Translations.Modal.Submission.Description.placeholder
			minLength = 1
			maxLength = 256
			required = true
		}

		val homepage = lineText {
			label = Translations.Modal.Submission.Homepage.label
			placeholder = Translations.Modal.Submission.Homepage.placeholder
			maxLength = 128
			required = false
		}

		val sourcecode = lineText {
			label = Translations.Modal.Submission.Source.extendedlabel
			placeholder = Translations.Modal.Submission.Source.placeholder
			maxLength = 128
			required = false
		}

		val downloadUrl = lineText {
			label = Translations.Modal.Submission.Downloadurl.label
			placeholder = Translations.Modal.Submission.Downloadurl.placeholder
			maxLength = 128
			required = false
		}
	}
}

private fun String?.convertBlankToNull(): String? {
	return if (this.isNullOrBlank()) { null } else { this }
}
