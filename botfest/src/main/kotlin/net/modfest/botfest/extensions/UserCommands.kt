package net.modfest.botfest.extensions

import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.application.slash.group
import dev.kordex.core.commands.converters.impl.optionalString
import dev.kordex.core.commands.converters.impl.string
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.i18n.withContext
import dev.kordex.core.koin.KordExKoinComponent
import net.modfest.botfest.MAIN_GUILD_ID
import net.modfest.botfest.Platform
import net.modfest.botfest.i18n.Translations
import net.modfest.platform.pojo.UserPatchData
import org.koin.core.component.inject

/**
 * Provides privileged commands that should be available only to admins
 */
class UserCommands : Extension(), KordExKoinComponent {
	override val name = "user"
	val platform: Platform by inject()

	override suspend fun setup() {
		ephemeralSlashCommand {
			name = Translations.Commands.Group.User.name
			description = Translations.Commands.Group.User.description

			guild(MAIN_GUILD_ID)  // Otherwise it will take up to an hour to update

			// Allows the user to change their data
			ephemeralSubCommand(::SetUserDataArgs) {
				name = Translations.Commands.User.Set.name
				description = Translations.Commands.User.Set.description

				action {
					var patch = UserPatchData(
						this.arguments.name,
						this.arguments.pronouns,
						this.arguments.bio
					)
					platform.withAuth(this.user).patchUserData(patch)

					respond {
						content = Translations.Commands.User.Set.response
							.withContext(this@action)
							.translateNamed()
					}
				}
			}

			group(Translations.Commands.Group.User.Minecraft.name) {
				description = Translations.Commands.Group.User.Minecraft.description

				// Allows the user to add a minecraft username
				ephemeralSubCommand(::MinecraftUsernameArgs) {
					name = Translations.Commands.User.Minecraft.Add.name
					description = Translations.Commands.User.Minecraft.Add.description

					action {
						val username = this.arguments.username
						platform.withAuth(this.user).addMinecraft(username)

						val user = platform.getUser(this.user)

						respond {
							content = Translations.Commands.User.Minecraft.Add.response
								.withContext(this@action)
								.translateNamed(
									"username" to username,
									"uuid" to user?.minecraftAccounts?.first() // Incorrect, but not sure how to fix.
								)
						}
					}
				}

				// Allows the user to remove a minecraft username
				ephemeralSubCommand(::MinecraftUsernameArgs) {
					name = Translations.Commands.User.Minecraft.Remove.name
					description = Translations.Commands.User.Minecraft.Remove.description

					action {
						val username = this.arguments.username
						platform.withAuth(this.user).removeMinecraft(username)

						respond {
							content = Translations.Commands.User.Minecraft.Remove.response
								.withContext(this@action)
								.translateNamed(
									"username" to username
								)
						}
					}
				}
			}
		}
	}

	inner class SetUserDataArgs : Arguments() {
		val name by optionalString {
			name = Translations.Arguments.Setuser.Name.name
			description = Translations.Arguments.Setuser.Name.description
		}
		val pronouns by optionalString {
			name = Translations.Arguments.Setuser.Pronouns.name
			description = Translations.Arguments.Setuser.Pronouns.description
		}
		val bio by optionalString {
			name = Translations.Arguments.Setuser.Bio.name
			description = Translations.Arguments.Setuser.Bio.description
		}
	}

	inner class MinecraftUsernameArgs : Arguments() {
		val username by string {
			name = Translations.Arguments.Minecraft.Username.name
			description = Translations.Arguments.Minecraft.Username.description
		}
	}
}
