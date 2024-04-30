package it.rattly.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.jte.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import it.rattly.plugins.AIRPORT_ANYWHERE
import it.rattly.plugins.AirportService
import it.rattly.plugins.Flight
import it.rattly.plugins.Trip
import it.skrape.core.document
import it.skrape.fetcher.AsyncFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import java.io.File

fun Application.configureFrontend() {
    routing {
        get("/scraped.html") {
            call.respondFile(File("scraped.html"))
        }

        get("/") {
            call.respond(JteContent("index.kte", mapOf("airports" to AirportService.getAirports())))
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
            call.respond(
                JteContent(
                    "partials/flightsPartial.kte", mapOf(
                        "flights" to mutableListOf<Trip>().apply {
                            repeat(100) {
                                add(
                                    Trip(
                                        departure = Flight(
                                            date = "2023-01-01",
                                            sourceAirport = AIRPORT_ANYWHERE,
                                            destinationAirport = AIRPORT_ANYWHERE,
                                            departureTime = "10:00",
                                            arrivalTime = "10:00",
                                            duration = "1",
                                            price = 1.00,
                                            company = "Ryanair",
                                            companyIata = "FR",
                                            cheapSeats = "1"
                                        ),
                                        arrival = Flight(
                                            date = "2023-01-01",
                                            sourceAirport = AIRPORT_ANYWHERE,
                                            destinationAirport = AIRPORT_ANYWHERE,
                                            departureTime = "10:00",
                                            arrivalTime = "10:00",
                                            duration = "1",
                                            price = 1.00,
                                            company = "Ryanair",
                                            companyIata = "FR",
                                            cheapSeats = "1"
                                        ),
                                        bookUrls = mapOf(
                                            listOf("test") to "https://example.com",
                                            listOf("test") to "https://example.com",
                                            listOf("test") to "https://example.com",
                                            listOf("test") to "https://example.com",
                                        )
                                    )
                                )
                            }
                        }
                    )
                )
            )
        }

        get<FlightsRequest> { req ->
            val invalidFields = mutableListOf<String>()

            if (req.sourceAirportId == null) invalidFields.add("sourceAirportId")
            if (req.destinationAirportId == null) invalidFields.add("destinationAirportId")
            if (req.adults == null) invalidFields.add("adults")
            if (req.children == null) invalidFields.add("children")
            if (req.infants == null) invalidFields.add("infants")
            if ((req.adults ?: 1) < 0) invalidFields.add("adults")
            if ((req.children ?: 1) < 0) invalidFields.add("children")
            if ((req.infants ?: 1) < 0) invalidFields.add("infants")

            if (invalidFields.isNotEmpty()) {
                call.respond(
                    JteContent(
                        "partials/errorPartial.kte",
                        mapOf("invalidFields" to invalidFields, "error" to "Correct the highlighted fields and try again.")
                    )
                )
            }

            val trips = req.fetchTrips() ?: run {
                call.respond(
                    JteContent(
                        "partials/errorPartial.kte",
                        mapOf(
                            "invalidFields" to null,
                            "error" to "No flights were found with your requested configuration. Please try again with a different combination of parameters."
                        )
                    )
                )
                return@get
            }

            call.respond(
                JteContent(
                    "partials/flightsPartial.kte", mapOf(
                        "flights" to trips
                    )
                )
            )
        }
    }
}