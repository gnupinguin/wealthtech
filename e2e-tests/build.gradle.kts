plugins {
    java
}

group = "io.gnupinguin.nevis"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.testng:testng:7.11.0")
    testImplementation("io.rest-assured:rest-assured:5.5.6")
    testImplementation("org.awaitility:awaitility:4.3.0")
    testImplementation("org.assertj:assertj-core:3.27.4")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.20.1")
    testImplementation("org.slf4j:slf4j-api:2.0.17")

    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.17")
}

tasks.named<Test>("test") {
    enabled = false
}

val testSourceSet = sourceSets.named("test")

tasks.register<Test>("e2eTest") {
    description = "Runs black-box end-to-end tests against a running WealthTech application."
    group = "verification"
    testClassesDirs = testSourceSet.get().output.classesDirs
    classpath = testSourceSet.get().runtimeClasspath
    shouldRunAfter(":integrationTest")

    useTestNG()

    val baseUrl = providers.gradleProperty("e2e.baseUrl")
        .orElse(providers.environmentVariable("E2E_BASE_URL"))
        .orElse("http://localhost:8080")
    systemProperty("e2e.baseUrl", baseUrl.get())

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
