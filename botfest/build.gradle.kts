import dev.kordex.gradle.plugins.docker.file.*
import dev.kordex.gradle.plugins.kordex.DataCollection

plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.serialization)

	alias(libs.plugins.shadow)
	alias(libs.plugins.detekt)

	alias(libs.plugins.kordex.docker)
	alias(libs.plugins.kordex.plugin)
}

group = "template"
version = "1.0-SNAPSHOT"

dependencies {
	detektPlugins(libs.detekt)

	implementation(libs.kotlin.stdlib)
	implementation(libs.kx.ser)

	// Logging dependencies
	implementation(libs.groovy)
	implementation(libs.jansi)
	implementation(libs.logback)
	implementation(libs.logback.groovy)
	implementation(libs.logging)
}

kordEx {
	// https://github.com/gradle/gradle/issues/31383
	kordExVersion = libs.versions.kordex.asProvider()

	bot {
		// See https://docs.kordex.dev/data-collection.html
		dataCollection(DataCollection.Standard)

		mainClass = "template.AppKt"
	}

	i18n {
		classPackage = "template.i18n"
		translationBundle = "template.strings"
	}
}

detekt {
	buildUponDefaultConfig = true

	config.from(project.files("detekt.yml"))
}

// Automatically generate a Dockerfile. Set `generateOnBuild` to `false` if you'd prefer to manually run the
// `createDockerfile` task instead of having it run whenever you build.
docker {
	generateOnBuild = false

	// Create the Dockerfile in build/Dockerfile.
	file(project.file("build/Dockerfile"))

	commands {
		// Each function (aside from comment/emptyLine) corresponds to a Dockerfile instruction.
		// See: https://docs.docker.com/reference/dockerfile/

		from("openjdk:21-jdk-slim")

		emptyLine()

		runShell("mkdir -p /bot/plugins")
		runShell("mkdir -p /bot/data")

		emptyLine()

		copy("build/libs/$name-*-all.jar", "/bot/bot.jar")

		emptyLine()

		// Add volumes for locations that you need to persist. This is important!
		volume("/bot/data")  // Storage for data files
		volume("/bot/plugins")  // Plugin ZIP/JAR location

		emptyLine()

		workdir("/bot")

		emptyLine()

		entryPointExec(
			"java", "-Xms2G", "-Xmx2G",
			"-jar", "/bot/bot.jar"
		)
	}
}
