package com.dannyprototype.carradio.data.repository

import com.dannyprototype.carradio.data.dao.FuelDao
import com.dannyprototype.carradio.data.entity.FuelEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FuelRepository @Inject constructor(
    private val fuelDao: FuelDao
) {
    fun getActiveBlockFlow(): Flow<FuelEntity?> = fuelDao.getActiveBlockFlow()

    suspend fun getActiveBlock(): FuelEntity? = fuelDao.getActiveBlock()

    suspend fun registerNewBlock(costDollars: Double, kmCapacity: Double): Long {
        fuelDao.deactivateAll()
        val entity = FuelEntity(
            costDollars = costDollars,
            kmCapacity = kmCapacity,
            costPerKm = if (kmCapacity > 0) costDollars / kmCapacity else 0.0,
            kmUsed = 0.0,
            isActive = true,
            dateRegistered = System.currentTimeMillis()
        )
        return fuelDao.insert(entity)
    }

    suspend fun updateKmUsed(blockId: Long, kmUsed: Double) {
        fuelDao.updateKmUsed(blockId, kmUsed)
    }

    fun getAllBlocks(): Flow<List<FuelEntity>> = fuelDao.getAll()
}
