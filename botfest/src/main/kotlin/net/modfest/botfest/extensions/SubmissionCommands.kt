package net.modfest.botfest.extensions

import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.behavior.interaction.response.edit
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.EphemeralSlashCommand
import dev.kordex.core.commands.application.slash.EphemeralSlashCommandContext
import dev.kordex.core.commands.application.slash.converters.ChoiceEnum
import dev.kordex.core.commands.application.slash.converters.impl.optionalEnumChoice
import dev.kordex.core.commands.application.slash.converters.impl.optionalNumberChoice
import dev.kordex.core.commands.application.slash.converters.impl.optionalStringChoice
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.application.slash.group
import dev.kordex.core.commands.converters.impl.attachment
import dev.kordex.core.commands.converters.impl.optionalAttachment
import dev.kordex.core.commands.converters.impl.optionalInt
import dev.kordex.core.commands.converters.impl.string
import dev.kordex.core.commands.converters.impl.user
import dev.kordex.core.components.components
import dev.kordex.core.components.ephemeralStringSelectMenu
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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.addAll
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import net.modfest.botfest.MAIN_GUILD_ID
import net.modfest.botfest.Platform
import net.modfest.botfest.i18n.Translations
import net.modfest.platform.pojo.SubmissionData.AssociatedData.Modrinth
import net.modfest.platform.pojo.SubmissionData.AssociatedData.Other
import net.modfest.platform.pojo.SubmissionData.BoothData
import net.modfest.platform.pojo.SubmissionData.BoothData.Column
import net.modfest.platform.pojo.SubmissionPatchData
import net.modfest.platform.pojo.SubmitRequestOther
import org.koin.core.component.inject
import java.nio.file.Files
import java.util.*
import java.util.regex.Pattern
import kotlin.io.path.Path

class SubmissionCommands : Extension(), KordExKoinComponent {
	override val name = "submission"
	val platform: Platform by inject()

	var MODRINTH_REGEX = Pattern.compile(".*modrinth\\.com/(project|mod|resourcepack|datapack|shader|plugin|modpack)/([\\w!@$()`.+,\"\\-']{3,64})/?.*")

	@OptIn(UnsafeAPI::class, ExperimentalSerializationApi::class)
	override suspend fun setup() {
		var slashIcon: EphemeralSlashCommand<ImageArg, ModalForm>? = null
		var slashScreenshot: EphemeralSlashCommand<ImageArg, ModalForm>? = null
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

					val projectSlug = matcher.group(2)

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
						content = Translations.Commands.Event.Submit.Other.Response.success
							.withContext(this@action)
							.translateNamed(
								"event" to eventInfo.name,
								"mod" to submission.name,
								"subId" to submission.id,
								"slashIcon" to slashIcon?.mention,
								"slashScreenshot" to slashScreenshot?.mention,
							)
					}
				}
			}

			// Submit an event / panel
			ephemeralSubCommand {
				name = Translations.Commands.Event.Submit.Event.name
				description = Translations.Commands.Event.Submit.Event.description

				action {
					respond {
						content = Translations.Modal.Submit.Event.Type.question
							.withContext(this@action)
							.translateNamed()
						components {
							ephemeralStringSelectMenu(::SubmitModalEvent) {
								option(Translations.Modal.Submit.Event.Type.Panel.label, "panel") {
									description = Translations.Modal.Submit.Event.Type.Panel.description
									emoji = DiscordPartialEmoji(name = "\uD83D\uDCE3")
								}
								option(Translations.Modal.Submit.Event.Type.Keynote.label, "keynote") {
									description = Translations.Modal.Submit.Event.Type.Keynote.description
									emoji = DiscordPartialEmoji(name = "\uD83D\uDCFA")
								}
								option(Translations.Modal.Submit.Event.Type.Qna.label, "qna") {
									description = Translations.Modal.Submit.Event.Type.Qna.description
									emoji = DiscordPartialEmoji(name = "\u2753")
								}
								option(Translations.Modal.Submit.Event.Type.Other.label, "other") {
									description = Translations.Modal.Submit.Event.Type.Other.description
									emoji = DiscordPartialEmoji(name = "\uD83D\uDCAD")
								}

								action { modal ->
									val menuResponse = this.selected
									if (modal == null) {
										respond {
											content = Translations.Modal.Submit.Event.Response.error
												.withContext(this@action)
												.translateNamed()
										}
									} else {
										Files.writeString(
											Path("./data/").resolve(UUID.randomUUID().toString()+".json"),
											buildJsonObject {
												putJsonArray("type") {
													addAll(menuResponse)
												}
												put("title", modal.event_title.value)
												put("description", modal.desc.value)
												put("collabs", modal.collabs.value)
												put("extra", modal.extra.value)
												put("submitter", this@action.user.id.toString())
											}.toString()
										)
										respond {
											content = Translations.Modal.Submit.Event.Response.success
												.withContext(this@action)
												.translateNamed()
										}
									}
								}
							}
						}
					}
				}
			}
		}

		// Other submission related commands
		ephemeralSlashCommand {
			name = Translations.Commands.Group.Submission.name
			description = Translations.Commands.Group.Submission.description

			guild(MAIN_GUILD_ID)

			// Edit submission data
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

					val submission = platform.getEventSubmissions(curEvent).find { it.id == subId }

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

					val result = modal.sendAndDeferEphemeral(this) ?: return@action

					platform.withAuth(this.user).editSubmissionData(curEvent, subId, SubmissionPatchData(
						modal.name.value,
						modal.description.value,
						modal.source.value,
						if (modal is SubmissionEditOtherForm) { modal.homepage.value } else { null },
						null
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

			// Delete submission data
			ephemeralSubCommand(::SubmissionArg) {
				name = Translations.Commands.Submission.Delete.name
				description = Translations.Commands.Submission.Delete.description

				action {
					val subId = this.arguments.submission
					val curEvent = platform.getCurrentEvent().event
					if (curEvent == null) {
						respond {
							content = Translations.Commands.Event.Submit.Response.unavailable
								.withContext(this@action)
								.translateNamed()
						}
						return@action
					}

					val submission = platform.getEventSubmissions(curEvent).find { it.id == subId }

					if (submission == null) {
						respond {
							content = Translations.Commands.Submission.Delete.Response.notfound
								.withContext(this@action)
								.translateNamed(
									"subId" to subId
								)
						}
						return@action
					}

					platform.withAuth(this.user).deleteSubmission(curEvent, subId)

					respond {
						content = Translations.Commands.Submission.Delete.Response.success
							.withContext(this@action)
							.translateNamed(
								"subId" to subId
							)
					}
				}
			}

			group(Translations.Commands.Group.Submission.Update.name) {
				description = Translations.Commands.Group.Submission.Update.description

				// Update submission version
				ephemeralSubCommand(::SubmissionArg) {
					name = Translations.Commands.Submission.Update.Version.name
					description = Translations.Commands.Submission.Update.Version.description

					action {
						val subId = this.arguments.submission
						val curEvent = platform.getCurrentEvent().event
						if (curEvent == null) {
							respond {
								content = Translations.Commands.Event.Submit.Response.unavailable
									.withContext(this@action)
									.translateNamed()
							}
							return@action
						}

						val submission = platform.getEventSubmissions(curEvent).find { it.id == subId }

						if (submission == null) {
							respond {
								content = Translations.Commands.Submission.Update.Version.Response.notfound
									.withContext(this@action)
									.translateNamed(
										"subId" to subId
									)
							}
							return@action
						}

						val subPlatformData = submission.platform.inner
						if (subPlatformData is Other) {
							val modal = SubmissionUpdateOtherForm()
							modal.downloadUrl.initialValue = subPlatformData.downloadUrl!!.toKey(Locale.UK)

							val result = modal.sendAndDeferEphemeral(this) ?: return@action

							platform.withAuth(this.user).editSubmissionData(curEvent, subId, SubmissionPatchData(
								null,
								null,
								null,
								null,
								modal.downloadUrl.value
							))

							result.edit {
								content = Translations.Commands.Submission.Edit.Response.success
									.withContext(this@action)
									.translateNamed(
										"subId" to subId
									)
							}
						} else if (submission.platform.inner !is Modrinth) {
							respond {
								content = Translations.Commands.Submission.Update.Version.Response.notmodrinth
									.withContext(this@action)
									.translateNamed(
										"subId" to subId
									)
							}
						} else {
							val updatedSubmission = platform.withAuth(this.user).updateSubmissionVersion(curEvent, subId)

							respond {
								content = Translations.Commands.Submission.Update.Version.Response.success
									.withContext(this@action)
									.translateNamed(
										"subId" to subId,
										"versionId" to (updatedSubmission.platform.inner as Modrinth).versionId
									)
							}
						}
					}
				}
				// Update submission meta
				ephemeralSubCommand(::SubmissionArg) {
					name = Translations.Commands.Submission.Update.Meta.name
					description = Translations.Commands.Submission.Update.Meta.description

					action {
						val subId = this.arguments.submission
						val curEvent = platform.getCurrentEvent().event
						if (curEvent == null) {
							respond {
								content = Translations.Commands.Event.Submit.Response.unavailable
									.withContext(this@action)
									.translateNamed()
							}
							return@action
						}

						val submission = platform.getEventSubmissions(curEvent).find { it.id == subId }

						if (submission == null) {
							respond {
								content = Translations.Commands.Submission.Update.Meta.Response.notfound
									.withContext(this@action)
									.translateNamed(
										"subId" to subId
									)
							}
							return@action
						}

						if (submission.platform.inner !is Modrinth) {
							respond {
								content = Translations.Commands.Submission.Update.Meta.Response.notmodrinth
									.withContext(this@action)
									.translateNamed(
										"subId" to subId
									)
							}
							return@action
						}

						platform.withAuth(this.user).updateSubmissionMeta(curEvent, subId)

						respond {
							content = Translations.Commands.Submission.Update.Meta.Response.success
								.withContext(this@action)
								.translateNamed(
									"subId" to subId
								)
						}
					}
				}
			}

			// Leave submission
			ephemeralSubCommand(::SubmissionArg) {
				name = Translations.Commands.Submission.Leave.name
				description = Translations.Commands.Submission.Leave.description

				action {
					val userId = this.user
					val subId = this.arguments.submission
					val curEvent = platform.getCurrentEvent().event
					if (curEvent == null) {
						respond {
							content = Translations.Commands.Event.Submit.Response.unavailable
								.withContext(this@action)
								.translateNamed()
						}
						return@action
					}

					val submission = platform.getEventSubmissions(curEvent).find { it.id == subId }

					if (submission == null) {
						respond {
							content = Translations.Commands.Submission.Leave.Response.notfound
								.withContext(this@action)
								.translateNamed(
									"subId" to subId
								)
						}
						return@action
					}

					val author = platform.getUser(userId)

					if (author == null) {
						respond {
							content = Translations.Commands.Submission.Invite.Response.usernotfound
								.withContext(this@action)
								.translateNamed(
									"userId" to userId.id.value
								)
						}
						return@action
					}

					if (!submission.authors.contains(author.id)) {
						respond {
							content = Translations.Commands.Submission.Leave.Response.notfound
								.withContext(this@action)
								.translateNamed(
									"subId" to subId
								)
						}
						return@action
					}

					if (submission.authors.size < 2) {
						respond {
							content = Translations.Commands.Submission.Leave.Response.last
								.withContext(this@action)
								.translateNamed(
									"subId" to subId
								)
						}
						return@action
					}

					platform.withAuth(this.user).leaveSubmission(curEvent, subId)

					respond {
						content = Translations.Commands.Submission.Leave.Response.success
							.withContext(this@action)
							.translateNamed(
								"subId" to subId
							)
					}
				}
			}

			// Invite user to submission
			ephemeralSubCommand(::InviteSubmissionArgs) {
				name = Translations.Commands.Submission.Invite.name
				description = Translations.Commands.Submission.Invite.description

				action {
					val subId = this.arguments.submission
					val userId = this.arguments.user
					val curEvent = platform.getCurrentEvent().event
					if (curEvent == null) {
						respond {
							content = Translations.Commands.Event.Submit.Response.unavailable
								.withContext(this@action)
								.translateNamed()
						}
						return@action
					}

					val submission = platform.getEventSubmissions(curEvent).find { it.id == subId }

					if (submission == null) {
						respond {
							content = Translations.Commands.Submission.Invite.Response.notfound
								.withContext(this@action)
								.translateNamed(
									"subId" to subId
								)
						}
						return@action
					}

					val author = platform.getUser(this.arguments.user)

					if (author == null) {
						respond {
							content = Translations.Commands.Submission.Invite.Response.usernotfound
								.withContext(this@action)
								.translateNamed(
									"userId" to userId.id.value
								)
						}
						return@action
					}

					if (submission.authors.contains(author.id)) {
						respond {
							content = Translations.Commands.Submission.Invite.Response.already
								.withContext(this@action)
								.translateNamed(
									"subId" to subId,
									"userId" to author.name
								)
						}
						return@action
					}

					platform.withAuth(this.user).inviteSubmissionAuthor(curEvent, subId, author.id)

					respond {
						content = Translations.Commands.Submission.Invite.Response.success
							.withContext(this@action)
							.translateNamed(
								"subId" to subId,
								"userId" to author.name
							)
					}
				}
			}

			// Edit submissions images
			group(Translations.Commands.Submission.EditImage.label) {
				description = Translations.Commands.Submission.EditImage.description

				slashIcon = ephemeralSubCommand(::ImageArg) {
					name = Translations.Commands.Submission.EditImage.Icon.label
					description = Translations.Commands.Submission.EditImage.Icon.description

					action {
						imageCommandAction("icon")
					}
				}

				slashScreenshot = ephemeralSubCommand(::ImageArg) {
					name = Translations.Commands.Submission.EditImage.Screenshot.label
					description = Translations.Commands.Submission.EditImage.Screenshot.description

					action {
						imageCommandAction("screenshot")
					}
				}
			}

			ephemeralSubCommand(::ImageArg) {
				name = Translations.Commands.Submission.Test.name
				description = Translations.Commands.Submission.Test.description

				action {
					imageCommandAction("test")
				}
			}

			ephemeralSubCommand(::ClaimArg) {
				name = Translations.Commands.Submission.Claim.name
				description = Translations.Commands.Submission.Claim.description

				action {
					val subId = this.arguments.submission
					val curEvent = platform.getCurrentEvent().event
					if (curEvent == null) {
						respond {
							content = Translations.Commands.Event.Submit.Response.unavailable
								.withContext(this@action)
								.translateNamed()
						}
						return@action
					}

					val submission = platform.getEventSubmissions(curEvent).find { it.id == subId }

					if (submission == null) {
						respond {
							content = Translations.Commands.Submission.Edit.Response.notfound
								.withContext(this@action)
								.translateNamed(
									"subId" to subId
								)
						}
						return@action
					}

					if (submission.boothData == null || submission.boothData?.markerPos == null) {
						// Must provide ALL data initially
						if (this.arguments.image == null ||
							this.arguments.markerX == null ||
							this.arguments.markerZ == null ||
							this.arguments.warpX == null ||
							this.arguments.warpY == null ||
							this.arguments.warpZ == null ||
							this.arguments.warpDirection == null
						) {
							respond {
								content = Translations.Commands.Submission.Claim.Response.incomplete
									.withContext(this@action)
									.translateNamed(
										"subId" to subId
									)
							}
							return@action
						}
					}

					if (this.arguments.image != null) {
						platform.withAuth(this.user).editSubmissionImage(curEvent, subId, "claim", this.arguments.image!!.url)
					}

					if (this.arguments.image != null ||
						this.arguments.markerX != null ||
						this.arguments.markerZ != null ||
						this.arguments.warpX != null ||
						this.arguments.warpY != null ||
						this.arguments.warpZ != null ||
						this.arguments.warpDirection != null
					) {
						platform.withAuth(this.user).editSubmissionBoothData(curEvent, subId, BoothData(
							Column(
								this.arguments.markerX ?: submission.boothData!!.markerPos!!.x,
								this.arguments.markerZ ?: submission.boothData!!.markerPos!!.z
							),
							BoothData.WarpCoordinates(
								this.arguments.warpX ?: submission.boothData!!.warp!!.x,
								this.arguments.warpY ?: submission.boothData!!.warp!!.y,
								this.arguments.warpZ ?: submission.boothData!!.warp!!.z,
								BoothData.WarpCoordinates.Direction.valueOf(this.arguments.warpDirection?.name ?: submission.boothData!!.warp!!.direction.name)
							),
							null,
							null,
							null,
							null
						))
					} else if (this.arguments.image == null) {
						respond {
							content = Translations.Commands.Submission.Claim.Response.unchanged
								.withContext(this@action)
								.translateNamed(
									"subId" to subId
								)
						}
						return@action
					}

					respond {
						content = Translations.Commands.Submission.Claim.Response.success
							.withContext(this@action)
							.translateNamed(
								"subId" to subId
							)
					}
				}
			}

			ephemeralSubCommand(::BuildArg) {
				name = Translations.Commands.Submission.Build.name
				description = Translations.Commands.Submission.Build.description

				action {
					val subId = this.arguments.submission
					val curEvent = platform.getCurrentEvent().event
					if (curEvent == null) {
						respond {
							content = Translations.Commands.Event.Submit.Response.unavailable
								.withContext(this@action)
								.translateNamed()
						}
						return@action
					}

					val submission = platform.getEventSubmissions(curEvent).find { it.id == subId }

					if (submission == null) {
						respond {
							content = Translations.Commands.Submission.Edit.Response.notfound
								.withContext(this@action)
								.translateNamed(
									"subId" to subId
								)
						}
						return@action
					}

					if (submission.boothData == null || submission.boothData?.itemIcon == null) {
						// Must provide ALL data initially
						if (this.arguments.image == null ||
							this.arguments.itemIcon == null ||
							this.arguments.shards == null ||
							this.arguments.eta == null ||
							this.arguments.status == null
						) {
							respond {
								content = Translations.Commands.Submission.Build.Response.incomplete
									.withContext(this@action)
									.translateNamed(
										"subId" to subId
									)
							}
							return@action
						}
					}

					if (this.arguments.image != null) {
						platform.withAuth(this.user).editSubmissionImage(curEvent, subId, "build", this.arguments.image!!.url)
					}

					if (this.arguments.image != null ||
						this.arguments.itemIcon != null ||
						this.arguments.shards != null ||
						this.arguments.eta != null ||
						this.arguments.status != null
					) {
						platform.withAuth(this.user).editSubmissionBoothData(curEvent, subId, BoothData(
							null,
							null,
							this.arguments.itemIcon,
							this.arguments.shards?.toInt(),
							this.arguments.eta,
							if (this.arguments.status == null) null else BoothData.BoothStatus.valueOf(this.arguments.status!!.name)
						))
					} else if (this.arguments.image == null) {
						respond {
							content = Translations.Commands.Submission.Build.Response.unchanged
								.withContext(this@action)
								.translateNamed(
									"subId" to subId
								)
						}
						return@action
					}

					respond {
						content = Translations.Commands.Submission.Build.Response.success
							.withContext(this@action)
							.translateNamed(
								"subId" to subId
							)
					}
				}
			}
		}
	}

	private suspend fun EphemeralSlashCommandContext<ImageArg, ModalForm>.imageCommandAction(type: String) {
		val subId = this.arguments.submission
		val curEvent = platform.getCurrentEvent().event
		if (curEvent == null) {
			respond {
				content = Translations.Commands.Event.Submit.Response.unavailable
					.withContext(this@imageCommandAction)
					.translateNamed()
			}
			return
		}

		val submission = platform.getEventSubmissions(curEvent).find { it.id == subId }

		if (submission == null) {
			respond {
				content = Translations.Commands.Submission.Edit.Response.notfound
					.withContext(this@imageCommandAction)
					.translateNamed(
						"subId" to subId
					)
			}
			return
		}

		platform.withAuth(this.user).editSubmissionImage(curEvent, subId, type, this.arguments.image.url)

		respond {
			content = Translations.Commands.Submission.EditImage.Response.success
				.withContext(this@imageCommandAction)
				.translateNamed()
		}
	}

	open inner class SubmissionArg : Arguments() {
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

	open inner class InviteSubmissionArgs : SubmissionArg() {
		val user by user {
			name = Translations.Arguments.Submission.Invite.User.name
			description = Translations.Arguments.Submission.Invite.User.description
		}
	}

	inner class ClaimArg : OptionalImageArg() {
		val markerX by optionalInt {
			name = Translations.Arguments.Submission.Claim.MarkerX.name
			description = Translations.Arguments.Submission.Claim.MarkerX.description
		}
		val markerZ by optionalInt {
			name = Translations.Arguments.Submission.Claim.MarkerZ.name
			description = Translations.Arguments.Submission.Claim.MarkerZ.description
		}
		val warpX by optionalInt {
			name = Translations.Arguments.Submission.Claim.WarpX.name
			description = Translations.Arguments.Submission.Claim.WarpX.description
		}
		val warpY by optionalInt {
			name = Translations.Arguments.Submission.Claim.WarpY.name
			description = Translations.Arguments.Submission.Claim.WarpY.description
		}
		val warpZ by optionalInt {
			name = Translations.Arguments.Submission.Claim.WarpZ.name
			description = Translations.Arguments.Submission.Claim.WarpZ.description
		}
		val warpDirection by optionalEnumChoice<WarpDirection> {
			name = Translations.Arguments.Submission.Claim.Direction.name
			description = Translations.Arguments.Submission.Claim.Direction.description
			typeName = Translations.Arguments.Submission.Claim.Direction.type
		}
	}

	inner class BuildArg : OptionalImageArg() {
		val itemIcon by optionalStringChoice {
			name = Translations.Arguments.Submission.Build.ItemIcon.name
			description = Translations.Arguments.Submission.Build.ItemIcon.description
		}
		val shards by optionalNumberChoice {
			name = Translations.Arguments.Submission.Build.Shards.name
			description = Translations.Arguments.Submission.Build.Shards.description
			choices = linkedMapOf(
				Translations.Arguments.Submission.Build.Shards.Choice.one to 1,
				Translations.Arguments.Submission.Build.Shards.Choice.two to 2,
				Translations.Arguments.Submission.Build.Shards.Choice.three to 3,
				Translations.Arguments.Submission.Build.Shards.Choice.four to 4
			)
		}
		val eta by optionalInt {
			name = Translations.Arguments.Submission.Build.Eta.name
			description = Translations.Arguments.Submission.Build.Eta.description
		}
		val status by optionalEnumChoice<BoothStatus> {
			name = Translations.Arguments.Submission.Build.Status.name
			description = Translations.Arguments.Submission.Build.Status.description
			typeName = Translations.Arguments.Submission.Build.Status.type
		}
	}

	enum class WarpDirection(override val readableName: Key) : ChoiceEnum {
			NORTH(Translations.Arguments.Submission.Claim.Direction.Choice.north),
			NORTH_NORTH_EAST(Translations.Arguments.Submission.Claim.Direction.Choice.northNorthEast),
			NORTH_EAST(Translations.Arguments.Submission.Claim.Direction.Choice.northEast),
			EAST_NORTH_EAST(Translations.Arguments.Submission.Claim.Direction.Choice.eastNorthEast),
			EAST(Translations.Arguments.Submission.Claim.Direction.Choice.east),
			EAST_SOUTH_EAST(Translations.Arguments.Submission.Claim.Direction.Choice.eastSouthEast),
			SOUTH_EAST(Translations.Arguments.Submission.Claim.Direction.Choice.southEast),
			SOUTH_SOUTH_EAST(Translations.Arguments.Submission.Claim.Direction.Choice.southSouthEast),
			SOUTH(Translations.Arguments.Submission.Claim.Direction.Choice.south),
			SOUTH_SOUTH_WEST(Translations.Arguments.Submission.Claim.Direction.Choice.southSouthWest),
			SOUTH_WEST(Translations.Arguments.Submission.Claim.Direction.Choice.southWest),
			WEST_SOUTH_WEST(Translations.Arguments.Submission.Claim.Direction.Choice.westSouthWest),
			WEST(Translations.Arguments.Submission.Claim.Direction.Choice.west),
			WEST_NORTH_WEST(Translations.Arguments.Submission.Claim.Direction.Choice.westNorthWest),
			NORTH_WEST(Translations.Arguments.Submission.Claim.Direction.Choice.northWest),
			NORTH_NORTH_WEST(Translations.Arguments.Submission.Claim.Direction.Choice.northNorthWest);
	}

	enum class BoothStatus(override val readableName: Key) : ChoiceEnum {
		UNDER_CONSTRUCTION(Translations.Arguments.Submission.Build.Status.Choice.constructing),
		PLAYABLE(Translations.Arguments.Submission.Build.Status.Choice.playable),
		COMPLETE(Translations.Arguments.Submission.Build.Status.Choice.complete)
	}

	open inner class OptionalImageArg : SubmissionArg() {
		val image by optionalAttachment {
			name = Translations.Arguments.Submission.EditImage.name
			description = Translations.Arguments.Submission.EditImage.description
			validate {
				value?.isImage ?: true
			}
		}
	}

	inner class ImageArg : SubmissionArg() {
		val image by attachment {
			name = Translations.Arguments.Submission.EditImage.name
			description = Translations.Arguments.Submission.EditImage.description
			validate {
				value.isImage
			}
		}
	}

	open class SubmissionEditForm : ModalForm() {
		override var title: Key = Translations.Modal.Submission.Edit.title

		val name = lineText {
			label = Translations.Modal.Submission.Name.label
			placeholder = Translations.Modal.Submission.Name.placeholder
			required = true
			translateInitialValue = false
		}

		val description = lineText {
			label = Translations.Modal.Submission.Description.label
			placeholder = Translations.Modal.Submission.Description.placeholder
			required = true
			translateInitialValue = false
		}

		val source = lineText {
			label = Translations.Modal.Submission.Source.label
			placeholder = Translations.Modal.Submission.Source.placeholder
			required = false
			translateInitialValue = false
		}
	}

	class SubmissionEditOtherForm : SubmissionEditForm() {
		val homepage = lineText {
			label = Translations.Modal.Submission.Homepage.label
			placeholder = Translations.Modal.Submission.Homepage.placeholder
			required = true
			translateInitialValue = false
		}
	}

	class SubmissionUpdateOtherForm : ModalForm() {
		override var title: Key = Translations.Modal.Update.title

		val downloadUrl = lineText {
			label = Translations.Modal.Submission.Downloadurl.label
			placeholder = Translations.Modal.Submission.Downloadurl.placeholder
			maxLength = 128
			required = false
		}
	}

	class SubmitModalModrinth : ModalForm() {
		override var title: Key = Translations.Modal.Submit.title

		val modrinthUrl = lineText {
			label = Translations.Modal.Submit.Url.label
			placeholder = Translations.Modal.Submit.Url.placeholder
			minLength = 10
			maxLength = 1024
			required = true
		}
	}

	class SubmitModalOther : ModalForm() {
		override var title: Key = Translations.Modal.Submit.title

		val name = lineText {
			label = Translations.Modal.Submission.Name.label
			placeholder = Translations.Modal.Submission.Name.placeholder
			minLength = 1
			maxLength = 1024
			required = true
		}

		val description = lineText {
			label = Translations.Modal.Submission.Description.label
			placeholder = Translations.Modal.Submission.Description.placeholder
			minLength = 1
			maxLength = 1024
			required = true
		}

		val homepage = lineText {
			label = Translations.Modal.Submission.Homepage.label
			placeholder = Translations.Modal.Submission.Homepage.placeholder
			maxLength = 1024
			required = false
		}

		val sourcecode = lineText {
			label = Translations.Modal.Submission.Source.extendedlabel
			placeholder = Translations.Modal.Submission.Source.placeholder
			maxLength = 1024
			required = false
		}

		val downloadUrl = lineText {
			label = Translations.Modal.Submission.Downloadurl.label
			placeholder = Translations.Modal.Submission.Downloadurl.placeholder
			maxLength = 1024
			required = false
		}
	}

	class SubmitModalEvent : ModalForm() {
		override var title: Key = Translations.Modal.Submit.Event.title

		val event_title = lineText {
			label = Translations.Modal.Submit.Event.Title.label
			placeholder = Translations.Modal.Submit.Event.Title.placeholder
			required = true
		}

		val desc = paragraphText {
			label = Translations.Modal.Submit.Event.Desc.label
			placeholder = Translations.Modal.Submit.Event.Desc.placeholder
			required = true
		}

		val collabs = paragraphText {
			label = Translations.Modal.Submit.Event.Collabs.label
			placeholder = Translations.Modal.Submit.Event.Collabs.placeholder
			required = false
		}

		val extra = paragraphText {
			label = Translations.Modal.Submit.Event.Extra.label
			placeholder = Translations.Modal.Submit.Event.Extra.placeholder
			required = false
		}
	}
}

private fun String?.convertBlankToNull(): String? {
	return if (this.isNullOrBlank()) { null } else { this }
}
