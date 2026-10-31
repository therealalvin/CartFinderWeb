package com.example.data.model

data class GolfCartLocation(
    val id: String = System.currentTimeMillis().toString(),
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val spotLabel: String = "",
    val note: String = ""
)

enum class DistanceUnit(val label: String, val shortLabel: String) {
    YARDS("Yards", "yd"),
    METERS("Meters", "m"),
    FEET("Feet", "ft")
}
