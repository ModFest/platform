package net.modfest.botfest

import dev.kord.common.entity.Snowflake
import dev.kordex.core.ExtensibleBot
import dev.kordex.core.utils.env
import dev.kordex.core.utils.loadModule
import net.modfest.botfest.extensions.AdminCommands
import net.modfest.botfest.extensions.DebugCommands

val PLATFORM_API_URL = env("PLATFORM_API")

val MAIN_GUILD_ID = Snowflake(
	env("MAIN_GUILD").toLong()  // Get the test server ID from the env vars or a .env file
)

private val TOKEN = env("TOKEN")   // Get the bot's token from the env vars or a .env file

suspend fun main() {
	val bot = ExtensibleBot(TOKEN) {
		hooks {
			beforeKoinSetup {
				loadModule {
					single {
						Platform(PLATFORM_API_URL)
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
		}
	}

	bot.start()
}
