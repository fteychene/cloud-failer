plugins {
    kotlin("jvm") version "1.6.0"
    application
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("com.google.cloud.tools.jib") version "2.7.0"

}

group = "fr.fteychene.teaching.cloud.failer"
version = "1.0-SNAPSHOT"

project.setProperty("mainClassName", "fr.fteychene.teaching.cloud.failer.MainKt")

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(platform("org.http4k:http4k-bom:4.17.1.0"))
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-server-netty")
    implementation("org.http4k:http4k-format-jackson")
    implementation("org.http4k:http4k-client-websocket")
    implementation("org.http4k:http4k-metrics-micrometer")
    implementation("io.micrometer:micrometer-registry-prometheus:1.7.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11+")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.2")
    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("ch.qos.logback:logback-classic:1.2.3")
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
        targetCompatibility = "11"
    }
}

jib {
    from {
        image = "openjdk:11-jdk-buster"
    }
    to {
        image = "fteychene/cloud-failer"
        tags = setOf("${project.version}", "latest")
    }
    container {
        jvmFlags = listOf("-Xms512m", "-Xdebug")
        mainClass = "fr.fteychene.teaching.cloud.failer.MainKt"
        ports = listOf("8080")
        format = com.google.cloud.tools.jib.api.buildplan.ImageFormat.OCI
    }
}
