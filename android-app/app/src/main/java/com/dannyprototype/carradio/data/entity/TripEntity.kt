package com.dannyprototype.carradio.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,               // TripMode name
    val startTime: Long,            // epoch millis
    val endTime: Long,              // epoch millis
    val distanceKm: Double,
    val durationSeconds: Int,
    val stoppedTimeSeconds: Int,
    val avgSpeedKmh: Double,
    val fuelCostPerKm: Double,
    val tripCost: Double,
    val income: Double,
    val profit: Double,
    val dayKmAccumulated: Double,
    val dateTag: String             // "YYYY-MM-DD" for grouping by day
)
