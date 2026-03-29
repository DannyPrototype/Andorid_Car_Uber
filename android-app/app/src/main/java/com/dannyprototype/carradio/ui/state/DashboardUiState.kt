package com.dannyprototype.carradio.ui.state

import com.dannyprototype.carradio.model.TripMode

data class DashboardUiState(
    // Mode
    val currentMode: TripMode = TripMode.PERSONAL,
    val tripActive: Boolean = false,

    // Speed
    val speedKmh: Int = 0,
    val isStopped: Boolean = false,
    val speedBarPercent: Int = 0,   // 0-100 for speed indicator

    // Distances
    val tripDistanceKm: Double = 0.0,
    val dayKm: Double = 0.0,

    // Times
    val tripTime: String = "00:00",
    val stoppedTime: String = "00:00",
    val stoppedPercent: Int = 0,
    val consecutiveStoppedSec: Int = 0,

    // Fuel
    val fuelBarPercent: Int = 0,
    val fuelCurrentKm: Double = 0.0,
    val fuelCapacityKm: Double = 0.0,
    val fuelRemainingKm: Double = 0.0,
    val fuelRemainingDollars: Double = 0.0,
    val fuelCostPerKm: Double = 0.0,
    val fuelBlockCost: Double = 0.0,
    val fuelIsWarning: Boolean = false,
    val fuelIsDanger: Boolean = false,
    val fuelAlertVisible: Boolean = false,
    val hasFuelBlock: Boolean = false,

    // Finances
    val uberIncome: Double = 0.0,
    val tripCost: Double = 0.0,
    val totalDayCost: Double = 0.0,
    val profit: Double = 0.0,

    // Per-mode summaries
    val personalKm: Double = 0.0,
    val uberPassengerKm: Double = 0.0,
    val uberPassengerTrips: Int = 0,
    val uberEmptyKm: Double = 0.0,

    // Mode stats for mode bar
    val currentModeKm: Double = 0.0,
    val currentModeTrips: Int = 0
)
