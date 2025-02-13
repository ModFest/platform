# BotFest

BotFest is ModFest's discord bot. It's written in Kotlin using [Kord Extensions](https://kordex.dev/).

## Dev env
BotFest needs a number of environment variables to run. To set it up, copy the `.env.example` template
into a new `.env` file. The following environment variables are defined:

* `TOKEN`: This is the bot token for your bot
* `MAIN_GUILD`: BotFest is designed to work in one server only. This will specify that guild. It's
  mainly used for role management, but also for slash command registration.
* `REGISTERED_ROLE`: The role assigned to people when they're registered for ModFest. *This can be left empty*

## Quick introduction
The entrypoint is located in `App.kt`. This is where the main bot is configured. In KordEx, bots consist
of a number of extensions, which each handle a different part of the bot, the extensions are registered here.

Additionally, `App.kt` also creates an instance of the `Platform` object. This is a wrapper for the http calls
to platform. The `Platform` object is made available to the extensions through [Koin](https://insert-koin.io/).
`App.kt` also specifies an error handler. Any exception thrown during command execution will lead there. The platform
wrapper will translate http error responses into java exceptions, and those will be turned into user-friendly strings in
the error handler.
