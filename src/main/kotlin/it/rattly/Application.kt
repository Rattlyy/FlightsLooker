package it.rattly

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.server.webjars.*
import io.ktor.util.*
import it.rattly.plugins.configureJte
import it.rattly.routes.configureApi
import it.rattly.routes.configureFrontend
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

val devMode get() = PlatformUtils.IS_DEVELOPMENT_MODE

fun main() {
    embeddedServer(CIO, port = System.getenv("PORT")?.toIntOrNull() ?: 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Webjars
    install(Webjars)
    // for render's checks
    install(AutoHeadResponse)
    // for HMR
    install(SSE)
    // for typesafe requests
    install(Resources)

    // call logging
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    // to return json responses
    install(ContentNegotiation) {
        json(json)
    }

    // compression
    install(Compression) {
        gzip()
        deflate()
    }

    routing {
        staticResources("/assets", "static")
    }

    configureJte()
    configureApi()
    configureFrontend()
}

val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
    isLenient = true
}