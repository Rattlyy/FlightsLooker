@file:UseSerializers(CustomDateSerializer::class)

package it.rattly.objects

import io.ktor.resources.*
import it.rattly.plugins.Airport
import it.rattly.plugins.AirportService
import it.rattly.plugins.TripService
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Resource("/flights")
@Serializable
class FlightsRequest(
    private val sourceAirportId: List<Int>? = null,
    private val destinationAirportId: List<Int>? = null,
    private val adults: Int? = null,
    private val children: Int? = null,
    private val infants: Int? = null,

    private val startDate: LocalDate? = null,
    private val endDate: LocalDate? = null,
    private val direct: Boolean? = false,
) {
    suspend fun fetchTrips(): MutableList<Trip>? = runCatching {
        val sourceAirports = sourceAirportId!!.mapNotNull { id -> AirportService.getAirports().find { it.id == id } }
        val destinationAirports =
            destinationAirportId!!.mapNotNull { id -> AirportService.getAirports().find { it.id == id } }

        TripService.fetchTrips(
            Airport(
                sourceAirports.first().name,
                sourceAirports.first().code,
                sourceAirports.takeLast(sourceAirports.size - 1).map { it.code }
            ),

            Airport(
                destinationAirports.first().name,
                destinationAirports.first().code,
                destinationAirports.takeLast(destinationAirports.size - 1).map { it.code }
            ),

            adults!!,
            children!!,
            infants!!,
            startDate!!,
            endDate!!,
            direct!!
        )
    }.getOrElse {
        it.printStackTrace()
        return null
    }

    fun validate() = mutableListOf<String>().apply {
        if (sourceAirportId == null) add("sourceAirportId") // Check if sourceAirportId is null
        if (destinationAirportId == null) add("destinationAirportId") // Check if destinationAirportId is null
        if (adults == null) add("adults") // Check if adults is null
        if (children == null) add("children") // Check if children is null
        if (infants == null) add("infants") // Check if infants is null
        if (startDate == null) add("startDate") // Check if startDate is null
        if (endDate == null) add("endDate") // Check if endDate is null
        if (startDate!! > endDate!!) add("startDate") // Check if startDate is greater than endDate
        if ((adults ?: 1) < 0) add("adults") // Check if adults is less than 0
        if ((children ?: 1) < 0) add("children") // Check if children is less than 0
        if ((infants ?: 1) < 0) add("infants") // Check if infants is less than 0
    }
}

class CustomDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
    private val format = LocalDate.Format { dayOfMonth(); char('/'); monthNumber(); char('/'); year() }

    override fun deserialize(decoder: Decoder) = LocalDate.parse(
        decoder.decodeString(), format
    )

    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(
        value.format(format)
    )
}