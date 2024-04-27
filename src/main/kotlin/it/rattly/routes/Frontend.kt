package it.rattly.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.jte.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import it.rattly.plugins.AirportService
import it.rattly.plugins.TripService
import java.io.File

fun Application.configureFrontend() {
    routing {
        get("/scraped.html") {
            call.respondFile(File("scraped.html"))
        }

        get("/") {
            call.respond(JteContent("index.kte", mapOf("airports" to AirportService.getAirports())))
        }

        get<FlightsRequest> { req ->
            call.respond(
                JteContent(
                    "flightsPartial.kte", mapOf(
                        "flights" to TripService.fetchTrips(
                            AirportService.getAirports().find { it.id == req.sourceAirportId }
                                ?: run { call.respond(HttpStatusCode.BadRequest); return@get },
                            AirportService.getAirports().find { it.id == req.destinationAirportId }
                                ?: run { call.respond(HttpStatusCode.BadRequest); return@get },

                            req.adults,
                            req.children,
                            req.infants
                        )
                    )
                )
            )
        }
    }
}