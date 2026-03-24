# ModFest Platform
This project contains the code for the [platform api](https://platform.modfest.net/) as well as the Discord bot
for ModFest. 

## General layout
The repository contains three subprojects:

 * `common`: Contains all java representations for things serialized, as well as any custom gson serializers.
  This allows the logic to be shared between platform and botfest
 * `platform_api`: The code for the web api. It handles our data, any business logic, and exposing the data via http 
  endpoints
 * `botfest`: The code for the discord bot. It interacts with the http endpoints exposed by platform, and exposes
  them as discord slash commands. It also contains any other code which is discord-specific. (Most importantly, role
  assignment)
 * `panel`: A web ui which allows team members to do administrative actions with a more user-friendly UI than discord
  slash commands provide

A conscious choice was made to have the discord bot interact via the http endpoints only. This allows us to dogfood
the http api and ensure it's capable. It also means that they can be hosted and restarted separately.

## Running the projects in dev
Use `./gradlew bootRun` to run the platform api and `./gradlew dev` to run BotFest.
Platform api and panel will run without any configuration, whilst BotFest needs some setup (see [here](./botfest/about.md#dev-env)).

Panel can be started with `pnpm dev`. It tries to connect to the local backend by default but you can run it on the
production backend using `DEV_SERVER_URL="https://platform.modfest.net" pnpm dev`

You can configure the port and address that the platform api will use in dev in the `gradle.properties` file. This
file is used in all subprojects.

## Running tests
Use `./gradlew test`. Current testing is quite limited.

## View the docs
You're viewing them right now! The docs are generated using [`mkdocs`](<https://www.mkdocs.org/>)
