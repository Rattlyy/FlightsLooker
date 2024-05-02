package it.rattly.plugins

import io.ktor.http.*
import it.rattly.devMode
import it.skrape.core.document
import it.skrape.fetcher.AsyncFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.File
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt

object TripService {
    suspend fun fetchTrips(
        sourceAirport: Airport = Airport("Bari", "BRI", listOf("BDS")),
        destinationAirport: Airport = Airport("Anywhere", "XXX"),
        adults: Int,
        children: Int,
        infants: Int,
    ) = skrape(AsyncFetcher /* uses coroutines */) {
        request {
            url = buildURL(sourceAirport, destinationAirport, adults, children, infants)
            timeout = 60 * 1000
        }

        response {
            val list = mutableListOf<Trip>()

            if (devMode) {
                CoroutineScope(Dispatchers.IO).launch {
                    File("scraped.html").writeText(document.toString())
                }
            }

            // scraped from azair's website, idk how it works but it works
            document.findAll("#reslist").first().children.filter { it.classNames.contains("result") }
                .forEach { res ->
                    val find = { className: String -> res.findAll(".$className").map { it.ownText } }
                    val aerCode = find("code")
                    val deptDate = find("date")
                    val deptTime = find("from")
                    val airlines = find("airline")
                    val arrTime = find("to")
                    val duration = find("durcha")
                    val priceForSepFl = find("legPrice")
                    val seatsForGivenPrice = find("icoSeatWrapper")
                    val lengthOfStay = find("lengthOfStay").first().replace("Length of stay: ", "").replace(" days", "")
                        .replace(" day", "").run { return@run if (this == "days") 1 else this.toInt() }

                    val airlineIata = res.findAll(".airline")
                        .map { it.classNames.filterNot { it.contains("airline") }.first().replace("iata", "") }

                    try {
                        list.add(
                            Trip(
                                departure = Flight(
                                    date = deptDate[0],
                                    sourceAirport = AirportService.getByCode(aerCode[0]) ?: AIRPORT_ANYWHERE,
                                    destinationAirport = AirportService.getByCode(aerCode[1]) ?: AIRPORT_ANYWHERE,
                                    departureTime = deptTime[1],
                                    arrivalTime = deptTime[3],
                                    duration = duration[0],
                                    company = airlines[0],
                                    companyIata = airlineIata[0],
                                    price = priceForSepFl[0].replace("€", "").toDouble().round(2),
                                    cheapSeats = seatsForGivenPrice[0],
                                ),

                                arrival = Flight(
                                    date = deptDate[1],
                                    sourceAirport = AirportService.getByCode(aerCode[4]) ?: AIRPORT_ANYWHERE,
                                    destinationAirport = AirportService.getByCode(aerCode[5]) ?: AIRPORT_ANYWHERE,
                                    departureTime = arrTime[0],
                                    arrivalTime = arrTime[2],
                                    company = airlines[1],
                                    companyIata = airlineIata[1],
                                    price = priceForSepFl[1].replace("€", "").toDouble().round(2),
                                    duration = duration[1],
                                    cheapSeats = seatsForGivenPrice[1]
                                ),

                                lengthOfStay = lengthOfStay,
                                bookUrls = res.findAll("a").map { it ->
                                    it.eachHref.filter { it.contains("book") } to (
                                            it.attributes["onclick"]
                                                ?.replace("trackBook('", "")
                                                ?.replace("')", "")
                                                ?.replace(",'", ",")
                                                ?.split("',")
                                                ?.filter { it.toIntOrNull() == null }
                                                ?.mapNotNull { AirportService.getByCode(it) }
                                                ?.joinToString(" -> ")
                                                ?: ""
                                            )
                                }.filterNot { it.first.isEmpty() }.distinct().toMap()
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            return@response list
        }
    }

    private fun buildURL(
        sourceAirport: Airport = Airport("Bari", "BRI", listOf("BDS")),
        destinationAirport: Airport = Airport("Anywhere", "XXX"),
        adults: Int,
        children: Int,
        infants: Int,
    ): String {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val month = Calendar.getInstance().get(Calendar.MONTH)
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val yearNew = Calendar.getInstance().get(Calendar.YEAR) + 1

        return URLBuilder(
            host = "www.azair.eu",
            protocol = URLProtocol.HTTPS,
        ).apply {
            // scraped from azair's website, idk how it works but it works
            path("azfin.php")
            parameters["tp"] = "0"
            parameters["searchtype"] = "flexi"
            parameters["srcAirport"] = sourceAirport.toString()
            parameters["srcTypedText"] = sourceAirport.code.lowercase()
            parameters["srcFreeTypedText"] = ""
            parameters["srcMC"] = ""

            sourceAirport.additionals.forEachIndexed { index, value ->
                parameters["srcap$index"] = value
            }

            parameters["srcFreeAirport"] = ""
            parameters["dstAirport"] = destinationAirport.toString()
            parameters["dstTypedText"] = destinationAirport.code.lowercase()
            parameters["dstFreeTypedText"] = ""
            parameters["dstMC"] = ""

            sourceAirport.additionals.forEachIndexed { index, value ->
                parameters["dstap$index"] = value
            }

            parameters["adults"] = "$adults"
            parameters["children"] = "$children"
            parameters["infants"] = "$infants"
            parameters["minHourStay"] = "0:45"
            parameters["maxHourStay"] = "23:20"
            parameters["minHourOutbound"] = "0:00"
            parameters["maxHourOutbound"] = "24:00"
            parameters["minHourInbound"] = "0:00"
            parameters["maxHourInbound"] = "24:00"
            parameters["depdate"] = "$day.$month.$year"
            parameters["arrdate"] = "$day.$month.$yearNew"
            parameters["minDaysStay"] = "1"
            parameters["maxDaysStay"] = "8"
            parameters["nextday"] = "0"
            parameters["autoprice"] = "true"
            parameters["currency"] = "EUR"
            parameters["wizzxclub"] = "false"
            parameters["flyoneclub"] = "false"
            parameters["blueairbenefits"] = "false"
            parameters["megavolotea"] = "false"
            parameters["schengen"] = "false"
            parameters["transfer"] = "false"
            parameters["samedep"] = "true"
            parameters["samearr"] = "true"
            parameters["dep0"] = "true"
            parameters["dep1"] = "true"
            parameters["dep2"] = "true"
            parameters["dep3"] = "true"
            parameters["dep4"] = "true"
            parameters["dep5"] = "true"
            parameters["dep6"] = "true"
            parameters["arr0"] = "true"
            parameters["arr1"] = "true"
            parameters["arr2"] = "true"
            parameters["arr3"] = "true"
            parameters["arr4"] = "true"
            parameters["arr5"] = "true"
            parameters["arr6"] = "true"
            parameters["maxChng"] = "1"
            parameters["isOneway"] = "return"
            parameters["resultSubmit"] = "Search"
        }.buildString()
    }
}

val PLACEHOLDER_TRIP = Trip(
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
    ),

    lengthOfStay = 1
)

@Serializable
data class Trip(
    val departure: Flight,
    val arrival: Flight,
    val lengthOfStay: Int,
    val totalPrice: Double = (departure.price + arrival.price).round(2),
    val bookUrls: Map<List<String>, String>
)

@Serializable
data class Flight(
    val sourceAirport: Airport,
    val destinationAirport: Airport,
    val departureTime: String,
    val arrivalTime: String,
    val date: String,
    val duration: String,
    val price: Double,
    val company: String,
    val companyIata: String,
    val cheapSeats: String
)

fun Double.round(digits: Int): Double {
    return (this * 10.0.pow(digits)).roundToInt() / 10.0.pow(digits)
}