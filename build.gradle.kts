import io.ktor.plugin.features.*

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val jte_version: String by project

// configurations.all {
//     resolutionStrategy.eachDependency {
//         if (requested.group == "io.ktor" && requested.name.contains("ktor-client")) {
//             useVersion("2.3.4")
//         }
//     }
// }

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
    mainClass.set("it.rattly.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
}

dependencies {
    implementation("io.ktor:ktor-server-cors")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-resources")
    implementation("io.ktor:ktor-server-host-common-jvm")
    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("io.ktor:ktor-server-call-id-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-cio-jvm")
    implementation("io.ktor:ktor-server-jte")
    implementation("io.ktor:ktor-server-auto-head-response")
    implementation("io.ktor:ktor-server-sse-jvm")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("com.github.kittinunf.fuel:fuel:2.+")
    // PSA: this is a patched version of skrapeit, it's not published on maven central yet
    // TODO: https://github.com/skrapeit/skrape.it/pull/239
    // TODO: fix when jitpack fixes itself
    implementation("it.skrape:skrapeit:1.3.0-alpha.2")
    implementation("gg.jte:jte:$jte_version")
    implementation("gg.jte:jte-watcher:$jte_version")
    implementation("gg.jte:jte-kotlin:$jte_version")
}

ktor {
    docker {
        localImageName = rootProject.name
        imageTag = "latest"

        jreVersion = JavaVersion.VERSION_17
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
    binaryStaticContent.set(true)
    generate()
}
