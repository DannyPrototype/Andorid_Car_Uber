package com.dannyprototype.carradio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dannyprototype.carradio.data.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Insert
    suspend fun insert(trip: TripEntity): Long

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun getAll(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE dateTag = :dateTag ORDER BY startTime ASC")
    fun getByDate(dateTag: String): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE mode = :mode ORDER BY startTime DESC")
    fun getByMode(mode: String): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE dateTag = :dateTag")
    suspend fun getTodayTripsSync(dateTag: String): List<TripEntity>

    @Query("SELECT COALESCE(SUM(distanceKm), 0.0) FROM trips WHERE dateTag = :dateTag")
    suspend fun getDayTotalKm(dateTag: String): Double

    @Query("SELECT COALESCE(SUM(income), 0.0) FROM trips WHERE dateTag = :dateTag")
    suspend fun getDayTotalIncome(dateTag: String): Double

    @Query("SELECT COUNT(*) FROM trips WHERE dateTag = :dateTag")
    suspend fun getDayTripCount(dateTag: String): Int
}
