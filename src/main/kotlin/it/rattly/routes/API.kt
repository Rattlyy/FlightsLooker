package it.rattly.routes

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import it.rattly.plugins.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

fun Application.configureApi() {
    routing {
        route("/api") {
            get<FlightsRequest> { req ->
                if (req.sourceAirportId == null) call.respond(
                    HttpStatusCode.BadRequest,
                    "The source airport id is not valid"
                )
                if (req.destinationAirportId == null) call.respond(
                    HttpStatusCode.BadRequest,
                    "The destination airport id is not valid"
                )
                if (req.adults == null) call.respond(HttpStatusCode.BadRequest, "The adults field is not valid")
                if (req.children == null) call.respond(HttpStatusCode.BadRequest, "The children field is not valid")
                if (req.infants == null) call.respond(HttpStatusCode.BadRequest, "The infants field is not valid")
                if (call.response.status() == HttpStatusCode.BadRequest) return@get

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

            //cacheOutput(1.days) {
            get("/airports") {
                call.respond(AirportService.getAirports())
            }
            //}
        }
    }
}

@Resource("/flights")
@Serializable
class FlightsRequest(
    val sourceAirportId: List<Int>? = null,
    val destinationAirportId: List<Int>? = null,
    val adults: Int? = null,
    val children: Int? = null,
    val infants: Int? = null,
) {
    suspend fun fetchTrips(): MutableList<Trip>? = runCatching {
        val sourceAirports = sourceAirportId!!.mapNotNull { id -> AirportService.getAirports().find { it.id == id } }
        val destinationAirports =
            destinationAirportId!!.mapNotNull { id -> AirportService.getAirports().find { it.id == id } }

        TripService.fetchTrips(
            Airport(
                sourceAirports.first().name,
                sourceAirports.first().code,
                sourceAirports.takeLast(sourceAirports.size - 1).map { it.code }
            ),

            Airport(
                destinationAirports.first().name,
                destinationAirports.first().code,
                destinationAirports.takeLast(destinationAirports.size - 1).map { it.code }
            ),

            adults!!,
            children!!,
            infants!!
        )
    }.getOrElse {
        it.printStackTrace()
        return null
    }
}