rootProject.name = "flightslooker"

pluginManagement {
    val ktorVersion: String by settings
    val kotlinVersion: String by settings
    val jteVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("io.ktor.plugin") version ktorVersion
        id("gg.jte.gradle") version jteVersion
    }
}