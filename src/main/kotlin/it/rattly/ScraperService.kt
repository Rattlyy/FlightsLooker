package it.rattly

import com.github.kittinunf.fuel.core.awaitResponseResult
import com.github.kittinunf.fuel.core.deserializers.StringDeserializer
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.getOrNull
import io.ktor.http.*
import it.skrape.core.document
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

object ScraperService {
    fun scrape(
        sourceAirport: Airport = Airport("Bari", "BRI", listOf("BDS")),
        destinationAirport: Airport = Airport("Anywhere", "XXX"),
        adults: Int,
        children: Int,
        infants: Int,
    ) = skrape(HttpFetcher) {
        request {
            url = buildURL(sourceAirport, destinationAirport, adults, children, infants)
            timeout = 60 * 1000
        }

        response {
            val list = mutableListOf<Trip>()

            //Files.write(Path.of("scraped.html"), document.toString().toByteArray())

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

                    try {
                        list.add(
                            Trip(
                                departure = Flight(
                                    date = deptDate[0],
                                    sourceAirport = aerCode[0],
                                    destinationAirport = aerCode[1],
                                    departureTime = deptTime[1],
                                    arrivalTime = deptTime[3],
                                    duration = duration[0],
                                    company = airlines[0],
                                    price = priceForSepFl[0].replace("€", "").toDouble().round(2),
                                    cheapSeats = seatsForGivenPrice[0],
                                ),

                                `return` = Flight(
                                    date = deptDate[1],
                                    sourceAirport = aerCode[4],
                                    destinationAirport = aerCode[5],
                                    departureTime = arrTime[0],
                                    arrivalTime = arrTime[2],
                                    company = airlines[1],
                                    price = priceForSepFl[1].replace("€", "").toDouble().round(2),
                                    duration = duration[1],
                                    cheapSeats = seatsForGivenPrice[1]
                                ),

                                bookUrls = mutableListOf<String>().apply {
                                    res.findAll("a").map { it -> it.eachHref.filter { it.contains("book") } }
                                        .filterNot { it.isEmpty() }.forEach { this@apply.addAll(it) }
                                }
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            return@response list
        }
    }

    suspend fun getAirports() =
        "https://static2.azair.us/www-azair-eu-assets/js/airports_array.js?1713856204"
            .httpGet().suspendable().awaitResponseResult(StringDeserializer()).third.getOrNull()
            ?.split("var airportsArray = ")?.get(1)
            ?.split(";")?.get(0)
            ?.replace("{", "")?.replace("}", "")
            ?.replace("\"", "")
            ?.replace(" ", "")
            ?.replace(",", "")
            ?.split("\n")
            ?.filterNot { it.isBlank() }
            ?.map { it.split(":") }
            ?.map { Airport(it[1], it[0]) }

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

        // https://www.azair.eu/azfin.php?searchtype=flexi&tp=0&isOneway=return&srcAirport=Perugia [PEG] (+AOI,CIA,FCO,PSR)&srcap0=AOI&srcap4=CIA&srcap5=FCO&srcap6=PSR&srcFreeAirport=&srcTypedText=pra&srcFreeTypedText=&srcMC=&dstAirport=Milan [MXP] (+LIN,BGY)&dstap0=LIN&dstap1=BGY&dstFreeAirport=&dstTypedText=mil&dstFreeTypedText=&dstMC=MIL_ALL&depmonth=202404&depdate=2024-04-25&aid=0&arrmonth=202503&arrdate=2025-03-02&minDaysStay=5&maxDaysStay=8&dep0=true&dep1=true&dep2=true&dep3=true&dep4=true&dep5=true&dep6=true&arr0=true&arr1=true&arr2=true&arr3=true&arr4=true&arr5=true&arr6=true&samedep=true&samearr=true&minHourStay=0:45&maxHourStay=23:20&minHourOutbound=0:00&maxHourOutbound=24:00&minHourInbound=0:00&maxHourInbound=24:00&autoprice=true&adults=1&children=0&infants=0&maxChng=1&currency=EUR&lang=en&indexSubmit=Search
        return URLBuilder(
            host = "www.azair.eu",
            protocol = URLProtocol.HTTPS,
        ).apply {
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

@Serializable
data class Airport(
    val name: String,
    val code: String,
    val additionals: List<String> = emptyList(),
    val id: Int = code.hashCode()
) {

    override fun toString() =
        "${name.capitalize()} [${code}]" + (if (additionals.isNotEmpty()) " +(${additionals.joinToString(",")})" else "")
}

@Serializable
data class Trip(
    val departure: Flight,
    val `return`: Flight,
    val totalPrice: Double = (departure.price + `return`.price).round(2),
    val bookUrls: List<String>
)

@Serializable
data class Flight(
    val sourceAirport: String,
    val destinationAirport: String,
    val departureTime: String,
    val arrivalTime: String,
    val date: String,
    val duration: String,
    val price: Double,
    val company: String,
    val cheapSeats: String
)

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }
}