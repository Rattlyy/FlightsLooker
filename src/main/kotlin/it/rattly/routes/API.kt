package it.rattly.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import it.rattly.objects.FlightsRequest
import it.rattly.objects.PLACEHOLDER_TRIP
import it.rattly.objects.Trip
import it.rattly.plugins.AirportService
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*

@OptIn(ExperimentalSerializationApi::class)
fun Application.configureApi() {
    routing {
        route("/api") {
            get<FlightsRequest> { req ->
                val invalidFields = req.validate()
                if (invalidFields.isNotEmpty()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        buildJsonObject {
                            put("error", "Correct the specified fields and try again.")
                            putJsonArray("fields") {
                                addAll(invalidFields)
                            }
                        }
                    )

                    return@get
                }

                call.respond(
                    req.fetchTrips() ?: run {
                        call.respond(HttpStatusCode.BadRequest)

                        buildJsonObject {
                            put("error", "Bad request")
                            putJsonArray("possibleCauses") {
                                add("The source airport id is not valid")
                                add("The destination airport id is not valid")
                                add("The adults field is not valid")
                                add("The children field is not valid")
                                add("The infants field is not valid")
                                add("The source and destination airports are the same")
                                add("Flight backend is down or not responding in 60 seconds")
                                add("No flights found")
                            }
                        }
                    }
                )
            }

            get("/mockFlights") {
                call.respond(mutableListOf<Trip>().apply {
                    repeat(100) {
                        add(PLACEHOLDER_TRIP)
                    }
                })
            }

            get("/airports") {
                call.respond(AirportService.getAirports())
            }

            get("/healthcheck") {
                call.respond(HttpStatusCode.OK, "ok")
            }
        }
    }
}