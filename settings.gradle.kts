pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()

		maven("https://snapshots-repo.kordex.dev")
		maven("https://releases-repo.kordex.dev")
	}
}

rootProject.name = "platform"

include("botfest")
include("common")
include("platform_api")
