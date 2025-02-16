import cz.habarta.typescript.generator.*
import java.util.regex.Pattern

plugins {
	`java-library`
	id("cz.habarta.typescript-generator") version "3.2.1263"
}

group = "net.modfest"
version = project.property("version")!!

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Contains tool-agnostic nullability annotations
	api("org.jspecify:jspecify:1.0.0")

	api("com.google.code.gson:gson:2.11.0")

	// Yes, I am using lombok :(
	compileOnly("org.projectlombok:lombok:1.18.36")
	annotationProcessor("org.projectlombok:lombok:1.18.36")
}

// Generate typescript file from java code
val typescriptUnprocessed = project.layout.buildDirectory.file("typescript/unprocessed.ts").get()
val typescriptDefinitions = rootDir.resolve("panel/src/generated/defs.ts")

tasks {
	generateTypeScript {
		classPatterns = listOf("net.modfest.platform.pojo.**")
		excludeClassPatterns = listOf("**TypeAdapter")
		customTypeNamingFunction = "(name, simpleName) => { return name.substring(26) } "
		jsonLibrary = JsonLibrary.gson
		outputKind = TypeScriptOutputKind.module
		outputFileType = TypeScriptFileType.implementationFile
		outputFile = typescriptUnprocessed.toString()
	}
}

abstract class FixTypescriptTask : DefaultTask() {
	@get:InputFile
	abstract var input: File
	@get:OutputFile
	abstract var output: File

	@TaskAction
	fun action() {
		val inputStr = input.readText()
		// Matches string literals.
		// String literals are used to map enums
		// We want our enums to use lower case strings
		val outputStr = Pattern.compile("\".*?\"")
			.matcher(inputStr)
			.replaceAll { it.group().lowercase() }
		output.writeText(outputStr)
	}
}
tasks.register<FixTypescriptTask>("fixTypescript") {
	input = typescriptUnprocessed.asFile
	output = typescriptDefinitions
}
tasks.generateTypeScript { finalizedBy("fixTypescript") }
