plugins {
	java
	id("org.springframework.boot") version "3.4.0"
	id("io.spring.dependency-management") version "1.1.6"
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
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// Swagger ui
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

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

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

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

tasks.bootRun {
	environment(
		"PLATFORM_DATADIR" to project.rootDir.resolve("run/data"),
		"PLATFORM_LOGSDIR" to project.rootDir.resolve("run/logs"),
		"PLATFORM_BOTFESTSECRET" to project.property("dev.botfest.shared-secret"),
		"SERVER_ADDRESS" to project.property("dev.api.address"),
		"SERVER_PORT" to project.property("dev.api.port"),
	)
}
