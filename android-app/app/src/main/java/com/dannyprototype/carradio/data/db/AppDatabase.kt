package com.dannyprototype.carradio.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dannyprototype.carradio.data.dao.FuelDao
import com.dannyprototype.carradio.data.dao.TripDao
import com.dannyprototype.carradio.data.entity.FuelEntity
import com.dannyprototype.carradio.data.entity.TripEntity

@Database(
    entities = [TripEntity::class, FuelEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun fuelDao(): FuelDao
}
