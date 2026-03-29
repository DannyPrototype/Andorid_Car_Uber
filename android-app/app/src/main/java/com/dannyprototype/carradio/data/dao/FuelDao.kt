package com.dannyprototype.carradio.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dannyprototype.carradio.data.entity.FuelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelDao {

    @Insert
    suspend fun insert(fuel: FuelEntity): Long

    @Update
    suspend fun update(fuel: FuelEntity)

    @Query("SELECT * FROM fuel_blocks WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveBlock(): FuelEntity?

    @Query("SELECT * FROM fuel_blocks WHERE isActive = 1 LIMIT 1")
    fun getActiveBlockFlow(): Flow<FuelEntity?>

    @Query("UPDATE fuel_blocks SET isActive = 0 WHERE isActive = 1")
    suspend fun deactivateAll()

    @Query("SELECT * FROM fuel_blocks ORDER BY dateRegistered DESC")
    fun getAll(): Flow<List<FuelEntity>>

    @Query("UPDATE fuel_blocks SET kmUsed = :kmUsed WHERE id = :id")
    suspend fun updateKmUsed(id: Long, kmUsed: Double)
}
