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

val assertjVersion: String by project
val awaitilityVersion: String by project
val jacksonDatabindVersion: String by project
val restAssuredVersion: String by project
val slf4jVersion: String by project
val testngVersion: String by project

dependencies {
    testImplementation("org.testng:testng:$testngVersion")
    testImplementation("io.rest-assured:rest-assured:$restAssuredVersion")
    testImplementation("org.awaitility:awaitility:$awaitilityVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:$jacksonDatabindVersion")
    testImplementation("org.slf4j:slf4j-api:$slf4jVersion")

    testRuntimeOnly("org.slf4j:slf4j-simple:$slf4jVersion")
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
