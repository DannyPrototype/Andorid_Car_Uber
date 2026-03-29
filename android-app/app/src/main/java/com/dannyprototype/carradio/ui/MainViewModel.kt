package com.dannyprototype.carradio.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dannyprototype.carradio.data.entity.FuelEntity
import com.dannyprototype.carradio.data.entity.TripEntity
import com.dannyprototype.carradio.data.repository.FuelRepository
import com.dannyprototype.carradio.data.repository.TripRepository
import com.dannyprototype.carradio.export.CsvExporter
import com.dannyprototype.carradio.location.DistanceCalculator
import com.dannyprototype.carradio.location.SpeedCalculator
import com.dannyprototype.carradio.model.LocationPoint
import com.dannyprototype.carradio.model.TripMode
import com.dannyprototype.carradio.ui.state.DashboardUiState
import com.dannyprototype.carradio.ui.state.GeoPoint
import com.dannyprototype.carradio.ui.state.MapUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val tripRepository: TripRepository,
    private val fuelRepository: FuelRepository,
    private val csvExporter: CsvExporter
) : AndroidViewModel(application) {

    // ========== UI State Flows ==========
    private val _dashboardState = MutableStateFlow(DashboardUiState())
    val dashboardState: StateFlow<DashboardUiState> = _dashboardState.asStateFlow()

    private val _mapState = MutableStateFlow(MapUiState())
    val mapState: StateFlow<MapUiState> = _mapState.asStateFlow()

    // ========== Internal State ==========
    private var currentMode = TripMode.PERSONAL
    private var tripActive = false
    private var tripStartTime = 0L

    // Speed
    private var currentSpeedKmh = 0.0
    private var smoothedSpeed = 0.0

    // Distance
    private var tripDistanceKm = 0.0
    private var dayKm = 0.0
    private var previousPoint: LocationPoint? = null
    private val routePoints = mutableListOf<GeoPoint>()

    // Time
    private var tripDurationSec = 0
    private var stoppedTimeSec = 0
    private var consecutiveStoppedSec = 0
    private var timerJob: Job? = null

    // Fuel
    private var activeFuelBlock: FuelEntity? = null
    private var fuelAlertShown = false

    // Finance
    private var uberIncomeToday = 0.0

    // Per-mode accumulators (in-memory for current session)
    private val modeKm = mutableMapOf(
        TripMode.PERSONAL to 0.0,
        TripMode.UBER_WITH_PASSENGER to 0.0,
        TripMode.UBER_WITHOUT_PASSENGER to 0.0
    )
    private val modeTrips = mutableMapOf(
        TripMode.PERSONAL to 0,
        TripMode.UBER_WITH_PASSENGER to 0,
        TripMode.UBER_WITHOUT_PASSENGER to 0
    )

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Load today's accumulated data from DB
            dayKm = tripRepository.getDayTotalKm()
            uberIncomeToday = tripRepository.getDayTotalIncome()

            // Load per-mode data from today's trips
            val todayTrips = tripRepository.getTodayTripsSync()
            for (trip in todayTrips) {
                val mode = try { TripMode.valueOf(trip.mode) } catch (_: Exception) { continue }
                modeKm[mode] = (modeKm[mode] ?: 0.0) + trip.distanceKm
                modeTrips[mode] = (modeTrips[mode] ?: 0) + 1
            }

            // Load active fuel block
            activeFuelBlock = fuelRepository.getActiveBlock()

            emitDashboardState()
        }
    }

    // ========== Location Updates (called from Service) ==========
    fun onLocationUpdate(point: LocationPoint) {
        if (!tripActive) return

        val prev = previousPoint

        // Calculate speed
        val rawSpeed = SpeedCalculator.getSpeedKmh(point, prev)
        smoothedSpeed = SpeedCalculator.smoothSpeed(smoothedSpeed, rawSpeed)
        currentSpeedKmh = smoothedSpeed

        // Calculate incremental distance
        if (prev != null) {
            val dist = DistanceCalculator.incrementalDistanceKm(prev, point)
            if (dist > 0) {
                tripDistanceKm += dist
                dayKm += dist
                modeKm[currentMode] = (modeKm[currentMode] ?: 0.0) + dist

                // Update fuel block
                updateFuelUsage(dist)
            }
        }

        previousPoint = point

        // Update route
        val geoPoint = GeoPoint(point.latitude, point.longitude)
        routePoints.add(geoPoint)

        // Update map state
        _mapState.value = MapUiState(
            currentPosition = geoPoint,
            routePoints = routePoints.toList(),
            speedKmh = currentSpeedKmh.toInt(),
            isMoving = !SpeedCalculator.isStopped(currentSpeedKmh),
            bearing = if (routePoints.size >= 2) {
                calculateBearing(
                    routePoints[routePoints.size - 2],
                    routePoints.last()
                )
            } else 0f
        )

        emitDashboardState()
    }

    // ========== Timer (1 second tick for duration/stopped) ==========
    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (!tripActive) break

                tripDurationSec++

                if (SpeedCalculator.isStopped(currentSpeedKmh)) {
                    stoppedTimeSec++
                    consecutiveStoppedSec++
                } else {
                    consecutiveStoppedSec = 0
                }

                emitDashboardState()
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    // ========== Fuel ==========
    private fun updateFuelUsage(distKm: Double) {
        val block = activeFuelBlock ?: return
        val newKmUsed = block.kmUsed + distKm
        activeFuelBlock = block.copy(kmUsed = newKmUsed)

        // Persist periodically (every ~100m)
        viewModelScope.launch {
            fuelRepository.updateKmUsed(block.id, newKmUsed)
        }

        // Check if fuel depleted
        if (newKmUsed >= block.kmCapacity && !fuelAlertShown) {
            fuelAlertShown = true
        }
    }

    fun registerFuel(costDollars: Double, kmCapacity: Double) {
        viewModelScope.launch {
            val id = fuelRepository.registerNewBlock(costDollars, kmCapacity)
            activeFuelBlock = FuelEntity(
                id = id,
                costDollars = costDollars,
                kmCapacity = kmCapacity,
                costPerKm = if (kmCapacity > 0) costDollars / kmCapacity else 0.0,
                kmUsed = 0.0,
                isActive = true,
                dateRegistered = System.currentTimeMillis()
            )
            fuelAlertShown = false
            emitDashboardState()
        }
    }

    fun dismissFuelAlert() {
        fuelAlertShown = false
        emitDashboardState()
    }

    // ========== Trip Actions ==========
    fun startTrip() {
        if (tripActive) return

        tripActive = true
        tripStartTime = System.currentTimeMillis()
        tripDistanceKm = 0.0
        tripDurationSec = 0
        stoppedTimeSec = 0
        consecutiveStoppedSec = 0
        currentSpeedKmh = 0.0
        smoothedSpeed = 0.0
        previousPoint = null
        routePoints.clear()

        startTimer()
        emitDashboardState()
    }

    fun endTrip(incomeAmount: Double = 0.0) {
        if (!tripActive) return

        tripActive = false
        currentSpeedKmh = 0.0
        smoothedSpeed = 0.0
        consecutiveStoppedSec = 0
        stopTimer()

        val costPerKm = activeFuelBlock?.costPerKm ?: 0.0
        val tripCost = tripDistanceKm * costPerKm
        val profit = incomeAmount - tripCost
        val avgSpeed = if (tripDurationSec > 0) {
            (tripDistanceKm / tripDurationSec) * 3600.0
        } else 0.0

        // Update mode counters
        modeTrips[currentMode] = (modeTrips[currentMode] ?: 0) + 1
        if (incomeAmount > 0) {
            uberIncomeToday += incomeAmount
        }

        // Save to database
        viewModelScope.launch {
            tripRepository.insertTrip(
                TripEntity(
                    mode = currentMode.name,
                    startTime = tripStartTime,
                    endTime = System.currentTimeMillis(),
                    distanceKm = tripDistanceKm,
                    durationSeconds = tripDurationSec,
                    stoppedTimeSeconds = stoppedTimeSec,
                    avgSpeedKmh = avgSpeed,
                    fuelCostPerKm = costPerKm,
                    tripCost = tripCost,
                    income = incomeAmount,
                    profit = profit,
                    dayKmAccumulated = dayKm,
                    dateTag = LocalDate.now().toString()
                )
            )
            emitDashboardState()
        }
    }

    /**
     * Returns trip data needed for the income dialog.
     */
    fun getTripSummaryForDialog(): Pair<Double, Double> {
        val costPerKm = activeFuelBlock?.costPerKm ?: 0.0
        return Pair(tripDistanceKm, tripDistanceKm * costPerKm)
    }

    fun isUberWithPassenger(): Boolean = currentMode == TripMode.UBER_WITH_PASSENGER

    fun isTripActive(): Boolean = tripActive

    // ========== Mode ==========
    fun changeMode() {
        if (tripActive) return
        currentMode = currentMode.next()
        emitDashboardState()
    }

    // ========== CSV Export ==========
    fun exportCsv(): String? {
        val trips = tripRepository.getAllTrips()
        // We need a synchronous list, use runBlocking-like approach via state
        var result: String? = null
        viewModelScope.launch {
            val allTrips = tripRepository.getTodayTripsSync()
            if (allTrips.isEmpty()) {
                result = null
            } else {
                result = csvExporter.export(getApplication(), allTrips)
            }
        }
        return result
    }

    fun exportCsvAsync(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val allTrips = tripRepository.getTodayTripsSync()
            if (allTrips.isEmpty()) {
                onResult(null)
            } else {
                val path = csvExporter.export(getApplication(), allTrips)
                onResult(path)
            }
        }
    }

    // ========== Emit Dashboard State ==========
    private fun emitDashboardState() {
        val fuel = activeFuelBlock
        val costPerKm = fuel?.costPerKm ?: 0.0
        val fuelUsed = fuel?.kmUsed ?: 0.0
        val fuelCapacity = fuel?.kmCapacity ?: 0.0
        val fuelRemKm = (fuelCapacity - fuelUsed).coerceAtLeast(0.0)
        val fuelRemDollars = fuelRemKm * costPerKm
        val fuelPct = if (fuelCapacity > 0) ((fuelUsed / fuelCapacity) * 100).toInt().coerceIn(0, 100) else 0

        val tripCost = tripDistanceKm * costPerKm
        val totalDayCost = dayKm * costPerKm
        val profit = uberIncomeToday - totalDayCost

        _dashboardState.value = DashboardUiState(
            currentMode = currentMode,
            tripActive = tripActive,
            speedKmh = currentSpeedKmh.toInt(),
            isStopped = tripActive && SpeedCalculator.isStopped(currentSpeedKmh),
            speedBarPercent = ((currentSpeedKmh / 80.0) * 100).toInt().coerceIn(0, 100),
            tripDistanceKm = tripDistanceKm,
            dayKm = dayKm,
            tripTime = formatTime(tripDurationSec),
            stoppedTime = formatTime(stoppedTimeSec),
            stoppedPercent = if (tripDurationSec > 0) {
                ((stoppedTimeSec.toDouble() / tripDurationSec) * 100).toInt()
            } else 0,
            consecutiveStoppedSec = consecutiveStoppedSec,
            fuelBarPercent = fuelPct,
            fuelCurrentKm = fuelUsed,
            fuelCapacityKm = fuelCapacity,
            fuelRemainingKm = fuelRemKm,
            fuelRemainingDollars = fuelRemDollars,
            fuelCostPerKm = costPerKm,
            fuelBlockCost = fuel?.costDollars ?: 0.0,
            fuelIsWarning = fuelPct in 70..89,
            fuelIsDanger = fuelPct >= 90,
            fuelAlertVisible = fuelAlertShown,
            hasFuelBlock = fuel != null,
            uberIncome = uberIncomeToday,
            tripCost = tripCost,
            totalDayCost = totalDayCost,
            profit = profit,
            personalKm = modeKm[TripMode.PERSONAL] ?: 0.0,
            uberPassengerKm = modeKm[TripMode.UBER_WITH_PASSENGER] ?: 0.0,
            uberPassengerTrips = modeTrips[TripMode.UBER_WITH_PASSENGER] ?: 0,
            uberEmptyKm = modeKm[TripMode.UBER_WITHOUT_PASSENGER] ?: 0.0,
            currentModeKm = modeKm[currentMode] ?: 0.0,
            currentModeTrips = modeTrips[currentMode] ?: 0
        )
    }

    // ========== Helpers ==========
    private fun formatTime(sec: Int): String {
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return if (h > 0) {
            String.format("%d:%02d:%02d", h, m, s)
        } else {
            String.format("%02d:%02d", m, s)
        }
    }

    private fun calculateBearing(from: GeoPoint, to: GeoPoint): Float {
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val y = kotlin.math.sin(dLon) * kotlin.math.cos(lat2)
        val x = kotlin.math.cos(lat1) * kotlin.math.sin(lat2) -
                kotlin.math.sin(lat1) * kotlin.math.cos(lat2) * kotlin.math.cos(dLon)
        return ((Math.toDegrees(kotlin.math.atan2(y, x)) + 360) % 360).toFloat()
    }
}
