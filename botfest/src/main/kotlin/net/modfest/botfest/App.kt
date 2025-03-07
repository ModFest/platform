package net.modfest.botfest

import com.google.gson.Gson
import dev.kord.common.entity.Snowflake
import dev.kord.rest.builder.message.embed
import dev.kordex.core.ExtensibleBot
import dev.kordex.core.i18n.withContext
import dev.kordex.core.utils.env
import dev.kordex.core.utils.envOrNull
import dev.kordex.core.utils.loadModule
import net.modfest.botfest.extensions.*
import net.modfest.botfest.i18n.Translations
import net.modfest.platform.pojo.PlatformErrorResponse
import net.modfest.platform.pojo.PlatformErrorResponse.AlreadyExists

val PLATFORM_API_URL = env("PLATFORM_API")
val PLATFORM_SHARED_SECRET = env("PLATFORM_SECRET")

val MAIN_GUILD_ID = Snowflake(
	env("MAIN_GUILD").toLong()  // Get the test server ID from the env vars or a .env file
)
val REGISTERED_ROLE = envOrNull("REGISTERED_ROLE")?.let {
	if (it.isBlank()) { null } else { Snowflake(it.toLong()) }
}

private val TOKEN = env("TOKEN")   // Get the bot's token from the env vars or a .env file

suspend fun main() {
	val bot = ExtensibleBot(TOKEN) {
		hooks {
			beforeKoinSetup {
				loadModule {
					single {
						Platform(PLATFORM_API_URL)
					}
					single {
						CommandReferences()
					}
				}
			}
		}

		applicationCommands {
			enabled = true
		}

		extensions {
			add(::DebugCommands)
			add(::AdminCommands)
			add(::UserCommands)
			add(::EventCommands)
			add(::RoleManager)
			add(::SubmissionCommands)
		}

		errorResponse { message, type ->
			if (type.error is PlatformException) {
				var data = (type.error as PlatformException).data
				content = when (data.type) {
					PlatformErrorResponse.ErrorType.EVENT_NO_EXIST -> Translations.Apierror.eventNoExists
						.translateNamed("n" to data.data.asString)
					PlatformErrorResponse.ErrorType.USER_NO_EXIST -> Translations.Apierror.userNoExists
						.translateNamed("n" to data.data.asString)
					PlatformErrorResponse.ErrorType.ALREADY_USED -> Translations.Apierror.alreadyUsed
						.translateNamed(
							"fieldname" to Gson().fromJson(data.data, AlreadyExists::class.java).fieldName,
							"content" to Gson().fromJson(data.data, AlreadyExists::class.java).content
						)
					PlatformErrorResponse.ErrorType.INTERNAL -> Translations.Apierror.internal
						.translateNamed("error" to data.data.asString)
				}
			}
		}

		about {
			general {
				message {
					embed {
						title = Translations.Commands.About.Embed.title
							.translateNamed()
						description = Translations.Commands.About.Embed.description
							.translateNamed()
					}
				}
			}
		}
	}

	bot.start()
}
