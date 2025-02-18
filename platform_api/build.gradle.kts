import dev.kordex.gradle.plugins.docker.file.*
import dev.kordex.gradle.plugins.docker.file.commands.HealthcheckCommand.Check.Option

plugins {
	java
	id("org.springframework.boot") version "3.4.0"
	id("io.spring.dependency-management") version "1.1.6"
	alias(libs.plugins.kordex.docker)
}

group = "net.modfest"
version = project.property("version")!!

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
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

dependencies {
	// Spring boot
	implementation("org.springframework.boot:spring-boot-starter-web")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// Swagger ui
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

	// Git integration
	implementation("org.eclipse.jgit:org.eclipse.jgit:7.1.0.202411261347-r")

	// Apache shiro provides the api's for authentication and authorization
	implementation("org.apache.shiro:shiro-spring:2.0.2:jakarta") {
		exclude("org.apache.shiro", "shiro-web")
	}
	implementation("org.apache.shiro:shiro-spring-boot-web-starter:2.0.2:jakarta"){
		exclude("org.apache.shiro", "shiro-web")
	}
	implementation("org.apache.shiro:shiro-spring-boot-starter:2.0.2:jakarta"){
		exclude("org.apache.shiro", "shiro-web")
	}
	implementation("org.apache.shiro:shiro-web:2.0.2:jakarta")

	// Modrinth api
	implementation("nl.theepicblock:DukeRinth:0.5.0")

	// Yes, I am using lombok :(
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	// Add common sources
	implementation(project(":common"))
}

tasks.processResources {
	filesMatching("application.properties") {
		expand(
			"build_version" to version
		)
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// Default properties when running in dev
tasks.bootRun {
	environment(
		"PLATFORM_DATADIR" to project.rootDir.resolve("run/data"),
		"PLATFORM_LOGSDIR" to project.rootDir.resolve("run/logs"),
		"PLATFORM_BOTFESTSECRET" to project.property("dev.botfest.shared-secret"),
		"SERVER_ADDRESS" to project.property("dev.api.address"),
		"SERVER_PORT" to project.property("dev.api.port"),
	)
}

tasks.createDockerfile { dependsOn("bootBuildImage") }
docker {
	generateOnBuild = false

	var dockerfileLocation = project.file("build/Dockerfile")
	// Create the Dockerfile in build/Dockerfile.
	file(dockerfileLocation)

	commands {
		// Each function (aside from comment/emptyLine) corresponds to a Dockerfile instruction.
		// See: https://docs.docker.com/reference/dockerfile/

		from("openjdk:21-jdk-slim")

		workdir("/app")

		runShell("groupadd --system --gid 1001 app")
		runShell("useradd --system --uid 1001 app")

		val filename = tasks.bootBuildImage.get().archiveFile.get()
			.asFile.relativeTo(dockerfileLocation.parentFile)
		copy("$filename", "/app/app.jar")

		runShell("mkdir -p /var/lib/platform/data")

		env {
			add("SERVER_PORT", "8080")
		}

		expose(8080)
		entryPointExec(
			"java", "-Xms2G", "-Xmx2G",
			"-jar", "/app/app.jar"
		)
		healthcheck {
			check {
				option(Option.Interval("10m"))
				option(Option.StartInterval("5s"))
				option(Option.Retries(2))
				option(Option.Timeout("5s"))
				cmdShell("curl -f http://localhost:8080/health || exit 1")
			}
		}
	}
}
