plugins {
    kotlin("jvm") version "1.4.32"
    application
    java
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.32")

    // Utils
    implementation("commons-cli:commons-cli:1.4")

    // Parser
    implementation("net.sourceforge.htmlunit:htmlunit:2.50.0")

    // Logging
    implementation("io.github.microutils:kotlin-logging:1.6.26")
    implementation("org.apache.logging.log4j:log4j-api:2.13.3")
    implementation("org.apache.logging.log4j:log4j-core:2.13.3")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.13.3")
}

application.applicationName = "altchain-explorer-verifier"
application.mainClassName = "altchain.explorer.verifier.AltchainExplorerVerifierKt"