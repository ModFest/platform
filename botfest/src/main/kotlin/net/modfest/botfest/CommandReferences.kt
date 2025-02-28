package net.modfest.botfest

import dev.kordex.core.commands.application.slash.SlashCommand

class CommandReferences {
	var registerCommand: SlashCommand<*, *, *>? = null
	var unregisterCommand: SlashCommand<*, *, *>? = null
}
