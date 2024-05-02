rootProject.name = "flightslooker"

pluginManagement {
    val ktor_version: String by settings
    val kotlin_version: String by settings
    val jte_version: String by settings

    plugins {
        kotlin("jvm") version kotlin_version
        id("io.ktor.plugin") version ktor_version
        id("gg.jte.gradle") version jte_version
        id("org.jetbrains.kotlin.plugin.serialization") version kotlin_version
    }
}