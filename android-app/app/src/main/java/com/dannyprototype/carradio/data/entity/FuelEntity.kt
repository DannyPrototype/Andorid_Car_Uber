package com.dannyprototype.carradio.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fuel_blocks")
data class FuelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val costDollars: Double,        // cost paid
    val kmCapacity: Double,         // km this fuel covers
    val costPerKm: Double,          // costDollars / kmCapacity
    val kmUsed: Double = 0.0,       // km driven on this block so far
    val isActive: Boolean = true,   // current active block
    val dateRegistered: Long        // epoch millis
)
