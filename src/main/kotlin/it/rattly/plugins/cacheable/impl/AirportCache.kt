package it.rattly.plugins.cacheable.impl

import com.github.kittinunf.fuel.core.awaitResponseResult
import com.github.kittinunf.fuel.core.deserializers.StringDeserializer
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.getOrNull
import it.rattly.plugins.cacheable.ItemCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

object AirportCache : ItemCache<Airport>("airports", Airport::class) {
    suspend fun code(code: String) = all().find { it.code == code }

    override suspend fun getFromSOT() = withContext(Dispatchers.IO) {
        "https://static2.azair.us/www-azair-eu-assets/js/airports_array.js"
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
            ?.sortedBy { it.toString() }
            ?: listOf(Airport("Bari", "BRI", listOf("BDS")), Airport("Anywhere", "XXX"))
    }
}

val AIRPORT_ANYWHERE = Airport("Anywhere", "XXX")

@Serializable
@Suppress("DEPRECATION")
data class Airport(
    val name: String,
    val code: String,
    val additionals: List<String> = emptyList(),
    val id: Int = code.hashCode()
) {
    override fun toString() =
        "${name.capitalize()} [${code}]" + (if (additionals.isNotEmpty()) " (+${additionals.joinToString(",")})" else "")
}