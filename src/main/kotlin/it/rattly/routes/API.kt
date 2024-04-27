package it.rattly.routes

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import it.rattly.plugins.AirportService
import it.rattly.plugins.ScraperService
import kotlinx.serialization.Serializable

fun Application.configureApi() {
    routing {
        route("/api") {
            get<FlightsRequest> { req ->
                call.respond(
                    ScraperService.scrape(
                        AirportService.getAirports().find { it.id == req.sourceAirportId }
                            ?: run { call.respond(HttpStatusCode.BadRequest); return@get },
                        AirportService.getAirports().find { it.id == req.destinationAirportId }
                            ?: run { call.respond(HttpStatusCode.BadRequest); return@get },

                        req.adults,
                        req.children,
                        req.infants
                    )
                )
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
    val sourceAirportId: Int,
    val destinationAirportId: Int,
    val adults: Int,
    val children: Int,
    val infants: Int,
)