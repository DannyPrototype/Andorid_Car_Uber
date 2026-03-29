package com.dannyprototype.carradio.model

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,        // millis
    val speedMps: Float,        // speed from GPS in m/s (-1 if unavailable)
    val accuracy: Float         // accuracy in meters
)
