package it.rattly.plugins.cacheable.impl

import com.github.kittinunf.fuel.core.awaitResponseResult
import com.github.kittinunf.fuel.core.deserializers.StringDeserializer
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.getOrNull
import it.rattly.plugins.cacheable.ItemCache
import kotlinx.serialization.Serializable

object IconCache : ItemCache<Icon>("icons", Icon::class) {
    override suspend fun getFromSOT() =
        "https://static6.azair.us/www-azair-eu-assets/css/airlines.css".httpGet()
            .awaitResponseResult(StringDeserializer()).third.getOrNull()
            ?.replace(" ", "")
            ?.replace("}", "")
            ?.split(".iata")
            ?.toMutableList()?.apply { removeFirst() }
            ?.associate { it.split("{").let { it[0] to it[1] } }
            ?.map { Icon(it.key, it.value) }
            ?: emptyList()
}

@Serializable
data class Icon(val code: String, val css: String)