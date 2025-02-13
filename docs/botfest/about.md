# BotFest

BotFest is ModFest's discord bot. It's written in Kotlin using [Kord Extensions](https://kordex.dev/).

## Quick introduction
The entrypoint is located in `App.kt`. This is where the main bot is configured. In KordEx, bots consist
of a number of extensions, which each handle a different part of the bot, the extensions are registered here.

Additionally, `App.kt` also creates an instance of the `Platform` object. This is a wrapper for the http calls
to platform. The `Platform` object is made available to the extensions through [Koin](https://insert-koin.io/).
`App.kt` also specifies an error handler. Any exception thrown during command execution will lead there. The platform
wrapper will translate http error responses into java exceptions, and those will be turned into user-friendly strings in
the error handler.
