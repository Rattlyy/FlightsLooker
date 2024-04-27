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
    kotlin("jvm") version "1.9.23"
    id("io.ktor.plugin") version "3.0.0-beta-1"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
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
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
    }
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
    implementation("io.ktor:ktor-server-sse-jvm")
    //implementation("com.ucasoft.ktor:ktor-simple-cache:0.+")
    //implementation("com.ucasoft.ktor:ktor-simple-memory-cache:0.+")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("com.github.kittinunf.fuel:fuel:2.+")
    implementation("it.skrape:skrapeit:1.3.0-alpha.2")
   // implementation("io.ktor:ktor-client-apache5:2.3.4")
   // implementation("io.ktor:ktor-client-core:2.3.4")
    implementation("gg.jte:jte:$jte_version")
    implementation("gg.jte:jte-watcher:$jte_version")
    implementation("gg.jte:jte-kotlin:$jte_version")
}
