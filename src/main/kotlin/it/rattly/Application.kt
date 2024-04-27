package it.rattly

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.sse.*
import it.rattly.plugins.configureJte
import it.rattly.routes.configureApi
import it.rattly.routes.configureFrontend
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import kotlin.math.pow
import kotlin.math.roundToInt

fun main() {
    embeddedServer(CIO, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(SSE)
    install(Resources)
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
        callIdMdc("call-id")
    }

    install(CallId) {
        header(HttpHeaders.XRequestId)
        verify { callId: String ->
            callId.isNotEmpty()
        }
    }

    install(ContentNegotiation) {
        json(json)
    }

//    install(SimpleCache) {
//        memoryCache {
//            invalidateAt = 10.seconds
//        }
//    }

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

fun Double.round(digits: Int): Double {
    return (this * 10.0.pow(digits)).roundToInt() / 10.0.pow(digits)
}