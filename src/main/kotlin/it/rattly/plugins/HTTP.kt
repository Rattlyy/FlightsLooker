package it.rattly.plugins

import com.ucasoft.ktor.simpleCache.cacheOutput
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import it.rattly.Airport
import it.rattly.Flight
import it.rattly.ScraperService
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.days

fun Application.configureHTTP() {
    val airports = mutableListOf<Airport>()

    routing {
        get<FlightsRequest> { req ->
            call.respond(
                ScraperService.scrape(
                    airports.find { it.id == req.sourceAirportId }
                        ?: run { call.respond(HttpStatusCode.BadRequest); return@get },
                    airports.find { it.id == req.destinationAirportId }
                        ?: run { call.respond(HttpStatusCode.BadRequest); return@get },

                    req.adults,
                    req.children,
                    req.infants
                )
            )
        }

        cacheOutput(1.days) {
            get("/airports") {
                airports.clear()
                airports.addAll(
                    ScraperService.getAirports() ?: listOf(
                        Airport("Bari", "BRI", listOf("BDS")),
                        Airport("Anywhere", "XXX")
                    )
                )

                call.respond(airports)
            }
        }
    }
}

@Resource("/flights")
@Serializable
class FlightsRequest(
    val sourceAirportId: Int,
    val destinationAirportId: Int,
    val adults: Int,
    val children: Int,
    val infants: Int,
)