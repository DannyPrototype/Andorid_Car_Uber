package com.dannyprototype.carradio.location

import com.dannyprototype.carradio.model.LocationPoint
import kotlin.math.*

object DistanceCalculator {

    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Haversine distance between two points in km.
     */
    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Total distance in km from a list of sequential points.
     */
    fun totalDistanceKm(points: List<LocationPoint>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 1 until points.size) {
            total += haversineKm(
                points[i - 1].latitude, points[i - 1].longitude,
                points[i].latitude, points[i].longitude
            )
        }
        return total
    }

    /**
     * Incremental distance between two consecutive points.
     * Filters out GPS jitter by ignoring movements < minDistanceKm
     * when accuracy is poor.
     */
    fun incrementalDistanceKm(
        prev: LocationPoint,
        current: LocationPoint,
        minDistanceKm: Double = 0.005  // 5 meters
    ): Double {
        val dist = haversineKm(
            prev.latitude, prev.longitude,
            current.latitude, current.longitude
        )
        // If accuracy is poor (>50m), require bigger movement to count
        val threshold = if (current.accuracy > 50f) minDistanceKm * 3 else minDistanceKm
        return if (dist >= threshold) dist else 0.0
    }
}
