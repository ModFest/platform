plugins {
	`java-library`
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
