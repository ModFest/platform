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

repositories {
	mavenCentral()
	maven {
		url = uri("https://maven.theepicblock.nl")
		content {
			includeGroup("nl.theepicblock")
		}
	}
}

group = "net.modfest"
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

	// Custom added dependencies (these are not part of the kordex template)
	implementation("io.ktor:ktor-serialization-gson:3.0.2")
	implementation("org.xerial:sqlite-jdbc:3.48.0.0")
	implementation("nl.theepicblock:java-sse-client:0.2.0")
	implementation(project(":common"))
}

kordEx {
	jvmTarget = 21

	// https://github.com/gradle/gradle/issues/31383
	kordExVersion = libs.versions.kordex.asProvider()

	module("dev-unsafe")

	bot {
		// See https://docs.kordex.dev/data-collection.html
		dataCollection(DataCollection.Standard)

		mainClass = "net.modfest.botfest.AppKt"
	}

	i18n {
		classPackage = "net.modfest.botfest.i18n"
		translationBundle = "botfest.strings"
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

// Configure platform when running in dev mode
// For some reason we need to do this roundabout way, because the dev task is not yet added
tasks.whenTaskAdded {
	if (this.name == "dev") {
		val address = project.property("dev.api.address")
		val port = project.property("dev.api.port")
		val env = (this as JavaExec).environment
		env["PLATFORM_API"] = "${address}:${port}"
		env["PLATFORM_SECRET"] = project.property("dev.botfest.shared-secret")
		env["ENVIRONMENT"] = "dev"
	}
}
