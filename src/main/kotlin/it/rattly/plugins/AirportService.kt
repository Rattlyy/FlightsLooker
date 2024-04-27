package it.rattly.plugins

import com.github.kittinunf.fuel.core.awaitResponseResult
import com.github.kittinunf.fuel.core.deserializers.StringDeserializer
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.getOrNull
import it.rattly.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalSerializationApi::class)
object AirportService {
    private val cacheFile = File("airports.json")
    private var currentlyFetching = false
    private var cache: AirportCache

    init {
        val fileCache = fetchCacheFromFile()

        cache = if (fileCache.lastUpdate + 1.days.inWholeMilliseconds > System.currentTimeMillis()) {
            fileCache
        } else {
            AirportCache(runBlocking { fetchAirports() }, System.currentTimeMillis())
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            json.encodeToStream(cache, cacheFile.outputStream())
        })
    }

    suspend fun getByCode(code: String) = getAirports().find { it.code == code }

    // hits cache if possible, otherwise fetches from the server
    suspend fun getAirports() =
        if (System.currentTimeMillis() - cache.lastUpdate > 1.days.inWholeMilliseconds && !currentlyFetching) {
            currentlyFetching = true
            cache = AirportCache(fetchAirports(), System.currentTimeMillis())
            currentlyFetching = false

            cache.airports
        } else cache.airports

    // tries to get the cache stored in the disk and returns it if it exists, otherwise returns an empty cache
    private fun fetchCacheFromFile(): AirportCache =
        if (cacheFile.exists())
            json.decodeFromStream<AirportCache>(cacheFile.inputStream())
        else AirportCache(emptyList(), 0)

    // fetches the airports from the server and returns them
    private suspend fun fetchAirports() = withContext(Dispatchers.IO) {
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
            ?.map { Airport(it[1].replace("(", " ("), it[0]) }
            ?: listOf(Airport("Bari", "BRI", listOf("BDS")), Airport("Anywhere", "XXX"))
    }

    @Serializable
    private data class AirportCache(
        val airports: List<Airport>,
        val lastUpdate: Long
    )
}

val AIRPORT_ANYWHERE = Airport("Anywhere", "XXX")

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