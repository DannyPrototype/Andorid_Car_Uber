package com.dannyprototype.carradio.location

import com.dannyprototype.carradio.model.LocationPoint

object SpeedCalculator {

    // Threshold: <= 2 km/h = stopped
    const val STOPPED_THRESHOLD_KMH = 2.0

    /**
     * Get speed in km/h from a location point.
     * Prefers GPS-provided speed, falls back to haversine calculation.
     */
    fun getSpeedKmh(current: LocationPoint, previous: LocationPoint?): Double {
        // GPS speed available and valid
        if (current.speedMps >= 0f) {
            return (current.speedMps * 3.6).coerceAtLeast(0.0)
        }

        // Fallback: calculate from distance and time
        if (previous == null) return 0.0
        val timeDeltaSec = (current.timestamp - previous.timestamp) / 1000.0
        if (timeDeltaSec <= 0) return 0.0

        val distKm = DistanceCalculator.haversineKm(
            previous.latitude, previous.longitude,
            current.latitude, current.longitude
        )
        val speedKmh = (distKm / timeDeltaSec) * 3600.0

        // Filter unrealistic speeds (> 200 km/h probably GPS glitch)
        return if (speedKmh > 200.0) 0.0 else speedKmh
    }

    /**
     * Smooth speed using exponential moving average.
     */
    fun smoothSpeed(currentSmoothed: Double, newReading: Double, alpha: Double = 0.3): Double {
        return currentSmoothed + alpha * (newReading - currentSmoothed)
    }

    fun isStopped(speedKmh: Double): Boolean {
        return speedKmh <= STOPPED_THRESHOLD_KMH
    }
}
