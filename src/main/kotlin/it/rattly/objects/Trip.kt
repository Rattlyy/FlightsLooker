package it.rattly.objects

import it.rattly.plugins.AIRPORT_ANYWHERE
import it.rattly.plugins.Airport
import it.rattly.plugins.round
import kotlinx.serialization.Serializable

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