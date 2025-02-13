KordEx makes all the strings returned via the bot translatable by default. In practice, this means
that all the string are defined in `resources/translations/botfest/strings.properties` and accessed using, for example,
`Translations.Commands.Group.Admin.description`. How this works is that KordEx will generate
code matching the translation strings. Note that your IDE might get confused until you build the project. Afterwards the
translations should show up just fine.

For more information on translations, formatting, etc, please refer to
the [KordEx documentation on the topic](https://docs.kordex.dev/internationalization.html)
