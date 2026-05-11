plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "io.gnupinguin.nevis"
version = "0.0.1-SNAPSHOT"
description = "WealthTech"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

val springAiVersion: String by project
val testcontainersVersion: String by project

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }
    implementation("org.springframework.boot:spring-boot-starter-jetty")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.kafka:spring-kafka")
    runtimeOnly("org.postgresql:postgresql")

    implementation("org.springframework.ai:spring-ai-starter-model-openai")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:$springAiVersion")
        mavenBom("org.testcontainers:testcontainers-bom:$testcontainersVersion")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("integration", "e2e")
    }
}

val testSourceSet = sourceSets.named("test")

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    testClassesDirs = testSourceSet.get().output.classesDirs
    classpath = testSourceSet.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("integration")
    }
    shouldRunAfter("test")
}

tasks.register("e2eTest") {
    description = "Runs black-box end-to-end tests against a running WealthTech application."
    group = "verification"
    dependsOn(":e2e-tests:e2eTest")
}
