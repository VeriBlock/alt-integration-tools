import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version kotlinVersion
    idea
    application
    kotlin("plugin.serialization") version kotlinVersion
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")

    // Dependency Injection
    implementation("io.insert-koin:koin-core:$koinVersion")

    // Configuration
    implementation("io.github.config4k:config4k:0.4.0")

    // HTTP API
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")

    // Database
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.xerial:sqlite-jdbc:$sqliteVersion")

    // Guava (for thread factory builders)
    implementation("com.google.guava:guava:23.0")

    // Time
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDateTimeVersion")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-hocon:$kotlinxSerializationVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")

    // Swagger/OpenAPI
    implementation("com.github.papsign:Ktor-OpenAPI-Generator:0.2-beta.13")

    // Utils
    implementation("commons-cli:commons-cli:1.4")

    // Parser
    implementation("net.sourceforge.htmlunit:htmlunit:2.50.0")

    // Logging
    implementation("io.github.microutils:kotlin-logging:1.6.26")
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
}

application.applicationName = "altchain-explorer-verifier"
application.mainClassName = "altchain.explorer.verifier.AltchainExplorerVerifierKt"