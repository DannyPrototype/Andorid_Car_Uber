package com.dannyprototype.carradio.data.repository

import com.dannyprototype.carradio.data.dao.TripDao
import com.dannyprototype.carradio.data.entity.TripEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepository @Inject constructor(
    private val tripDao: TripDao
) {
    private fun todayTag(): String = LocalDate.now().toString()

    fun getAllTrips(): Flow<List<TripEntity>> = tripDao.getAll()

    fun getTodayTrips(): Flow<List<TripEntity>> = tripDao.getByDate(todayTag())

    fun getTripsByMode(mode: String): Flow<List<TripEntity>> = tripDao.getByMode(mode)

    suspend fun insertTrip(trip: TripEntity): Long = tripDao.insert(trip)

    suspend fun getTodayTripsSync(): List<TripEntity> = tripDao.getTodayTripsSync(todayTag())

    suspend fun getDayTotalKm(): Double = tripDao.getDayTotalKm(todayTag())

    suspend fun getDayTotalIncome(): Double = tripDao.getDayTotalIncome(todayTag())

    suspend fun getDayTripCount(): Int = tripDao.getDayTripCount(todayTag())
}
