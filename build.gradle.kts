import io.ktor.plugin.features.*

val isDevelopment: Boolean = project.ext.has("development")
val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project
val jteVersion: String by project

plugins {
    kotlin("jvm")
    id("io.ktor.plugin")
    id("gg.jte.gradle")
    id("org.jetbrains.kotlin.plugin.serialization")

    id("co.uzzu.dotenv.gradle") version "4.0.0"
}

group = "it.rattly"
version = "0.0.1"

application {
    mainClass = "it.rattly.ApplicationKt"
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0-RC.2")
    implementation("io.ktor:ktor-server-cors")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-resources")
    implementation("io.ktor:ktor-server-host-common-jvm")
    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-cio-jvm")
    implementation("io.ktor:ktor-server-jte")
    implementation("io.ktor:ktor-server-auto-head-response")
    implementation("io.ktor:ktor-server-sse-jvm")
    implementation("com.github.kittinunf.fuel:fuel:2.+")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("gg.jte:jte:$jteVersion")
    implementation("gg.jte:jte-watcher:$jteVersion")
    implementation("gg.jte:jte-kotlin:$jteVersion")

    // PSA: this is a patched version of skrapeit, it's not published on maven central yet
    // TODO: https://github.com/skrapeit/skrape.it/pull/239
    // TODO: fix when jitpack fixes itself
    implementation("it.skrape:skrapeit:1.3.0-alpha.2")
}


ktor {
    docker {
        jreVersion = JavaVersion.VERSION_17
        localImageName = rootProject.name
        imageTag = "latest"

        portMappings = listOf(
            DockerPortMapping(
                80,
                8080,
                DockerPortMappingProtocol.TCP
            )
        )

        externalRegistry = DockerImageRegistry.externalRegistry(
            hostname = provider { env.REGISTRY_HOSTNAME.value },
            username = provider { env.REGISTRY_USERNAME.value },
            password = provider { env.REGISTRY_PASSWORD.value },
            namespace = provider { env.REGISTRY_NAMESPACE.value },
            project = provider { rootProject.name }
        )
    }
}

jte {
    if (!isDevelopment) {
        binaryStaticContent.set(true)
        generate()
    }
}
