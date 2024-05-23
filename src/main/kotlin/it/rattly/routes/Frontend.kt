package it.rattly.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.jte.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import it.rattly.objects.FlightsRequest
import it.rattly.objects.PLACEHOLDER_TRIP
import it.rattly.objects.Trip
import it.rattly.plugins.cacheable.impl.AirportCache
import it.skrape.core.document
import it.skrape.fetcher.AsyncFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import java.io.File

fun Application.configureFrontend() {
    routing {
        get("/") {
            call.respondTemplate("index.kte", mapOf("airportsWithEverywhere" to AirportCache.all()))
        }

        get("/doBooking") {
            val toScrape = "https://azair.eu/" + (call.request.queryParameters["url"]?.decodeBase64String()
                ?: run { call.respond(HttpStatusCode.BadRequest); return@get })

            skrape(AsyncFetcher /* uses coroutines */) {
                request {
                    url = toScrape
                    timeout = 60 * 1000
                    headers {
                        append(
                            "User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36"
                        )

                        append(
                            "Accept",
                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
                        )

                        append("Accept-Language", "en-US,en;q=0.9")
                        append("Accept-Encoding", "gzip, deflate, br")
                        append("Origin", "https://azair.eu")
                        append("Connection", "keep-alive")
                        append("Referer", "https://azair.eu/azfin.php")
                    }
                }

                response {
                    call.respondBytes(
                        contentType = ContentType.Text.Html,
                        status = HttpStatusCode.OK,
                        bytes = document.toString().encodeToByteArray()
                    )
                }
            }
        }

        get("/mockFlights") {
            call.respondTemplate(
                "partials/flightsPartial.kte", mapOf(
                    "flights" to mutableListOf<Trip>().apply {
                        repeat(100) {
                            add(PLACEHOLDER_TRIP)
                        }
                    }
                )
            )
        }

        get<FlightsRequest> { req ->
            val invalidFields = req.validate()
            if (invalidFields.isNotEmpty()) {
                call.respondTemplate(
                    "partials/errorPartial.kte",

                    mapOf(
                        "invalidFields" to invalidFields,
                        "error" to "Correct the highlighted fields and try again."
                    )
                )
            }

            val trips = req.fetchTrips() ?: run {
                call.respondTemplate(
                    "partials/errorPartial.kte",

                    mapOf(
                        "invalidFields" to null,
                        "error" to "No flights were found with your requested configuration. Please try again with a different combination of parameters."
                    )
                )

                return@get
            }

            call.respondTemplate(
                "partials/flightsPartial.kte",

                mapOf("flights" to when (call.request.queryParameters["sorting"]) {
                    "price" -> trips.sortedBy { it.totalPrice }
                    "duration" -> trips.sortedBy { it.lengthOfStay }
                    "priceDesc" -> trips.sortedByDescending { it.totalPrice }
                    "durationDesc" -> trips.sortedByDescending { it.lengthOfStay }
                    else -> trips
                })
            )
        }

        if (developmentMode) {
            get("/dev/scraped") {
                call.respondFile(File("scraped.html"))
            }
        }
    }
}