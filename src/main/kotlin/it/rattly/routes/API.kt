package it.rattly.routes

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import it.rattly.plugins.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
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
        }
    }
}

@Resource("/flights")
@Serializable
class FlightsRequest(
    private val sourceAirportId: List<Int>? = null,
    private val destinationAirportId: List<Int>? = null,
    private val adults: Int? = null,
    private val children: Int? = null,
    private val infants: Int? = null,
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

    fun validate() = mutableListOf<String>().apply {
        if (sourceAirportId == null) add("sourceAirportId")
        if (destinationAirportId == null) add("destinationAirportId")
        if (adults == null) add("adults")
        if (children == null) add("children")
        if (infants == null) add("infants")

        if ((adults ?: 1) < 0) add("adults")
        if ((children ?: 1) < 0) add("children")
        if ((infants ?: 1) < 0) add("infants")
    }
}