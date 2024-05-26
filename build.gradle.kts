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
    //TODO: remove when skrapeit is published on maven central
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
    implementation("io.ktor:ktor-server-compression")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-cio-jvm")
    implementation("io.ktor:ktor-server-jte")
    implementation("io.ktor:ktor-server-auto-head-response")
    implementation("io.ktor:ktor-server-sse-jvm")
    implementation("io.ktor:ktor-server-webjars")
    implementation("com.github.kittinunf.fuel:fuel:2.+")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("gg.jte:jte:$jteVersion")
    implementation("gg.jte:jte-watcher:$jteVersion")
    implementation("gg.jte:jte-kotlin:$jteVersion")
    implementation("it.skrape:skrapeit:1.3.0-alpha.2")

    runtimeOnly("org.webjars.npm:bootstrap:5.3.3")
    runtimeOnly("org.webjars.npm:select2:4.1.0-rc.0")
    runtimeOnly("org.webjars.npm:select2-bootstrap-5-theme:1.3.0")
    runtimeOnly("org.webjars.npm:vanillajs-datepicker:1.3.4")
    runtimeOnly("org.webjars.npm:jquery:4.0.0-beta")
    runtimeOnly("org.webjars.npm:htmx.org:2.0.0-beta3")
}

jib {
    from {
        platforms {
            platform { os = "linux"; architecture = "arm64" }
        }
    }
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
