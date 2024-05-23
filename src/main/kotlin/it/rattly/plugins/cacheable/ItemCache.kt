package it.rattly.plugins.cacheable

import it.rattly.json
import it.rattly.plugins.cacheable.ItemCache.InternalCache.Companion.serializer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer
import java.io.File
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
abstract class ItemCache<T : Any>(val name: String, val classT: KClass<T>) {
    private val cacheFile = File(File("caches").apply { mkdirs() }, "$name.json")
    private var currentlyFetching = false
    private var cache: InternalCache<T>

    init {
        val fileCache = fetchCacheFromFile()

        // if the cache file is empty or expired, fetch the data from the server
        cache = if (fileCache.lastUpdate + 1.days.inWholeMilliseconds > System.currentTimeMillis()) {
            fileCache
        } else {
            InternalCache(runBlocking { getFromSOT() }, System.currentTimeMillis())
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            json.encodeToStream(
                serializer(classT.serializer()), cache, cacheFile.outputStream()
            )
        })
    }

    abstract suspend fun getFromSOT(): List<T>

    // hits cache if possible, otherwise fetches from the server
    suspend fun all() =
        if (System.currentTimeMillis() - cache.lastUpdate > 1.days.inWholeMilliseconds && !currentlyFetching) {
            currentlyFetching = true
            cache = InternalCache(getFromSOT(), System.currentTimeMillis())
            currentlyFetching = false

            cache.items
        } else cache.items

    // tries to get the cache stored in the disk and returns it if it exists, otherwise returns an empty cache
    private fun fetchCacheFromFile(): InternalCache<T> =
        if (cacheFile.exists())
            json.decodeFromStream(serializer(classT.serializer()), cacheFile.inputStream())
        else InternalCache(emptyList(), 0)

    @Serializable
    private data class InternalCache<T : Any>(
        val items: List<T>,
        val lastUpdate: Long
    )
}