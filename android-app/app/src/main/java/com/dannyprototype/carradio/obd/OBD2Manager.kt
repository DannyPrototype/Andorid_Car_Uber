package com.dannyprototype.carradio.obd

import kotlinx.coroutines.flow.Flow

/**
 * Interface for OBD2 adapter integration.
 * Future implementation will connect via Bluetooth to an ELM327 adapter
 * and read real-time vehicle data.
 *
 * Supported PIDs planned:
 * - 0x0D: Vehicle speed
 * - 0x0C: Engine RPM
 * - 0x2F: Fuel tank level
 * - 0x05: Engine coolant temperature
 * - 0x0F: Intake air temperature
 */
interface OBD2Manager {

    /** Connect to OBD2 adapter via Bluetooth */
    suspend fun connect(deviceAddress: String): Boolean

    /** Disconnect from adapter */
    suspend fun disconnect()

    /** True if currently connected */
    fun isConnected(): Boolean

    /** Vehicle speed in km/h */
    fun getSpeed(): Flow<Int>

    /** Engine RPM */
    fun getRPM(): Flow<Int>

    /** Fuel tank level as percentage 0.0 - 100.0 */
    fun getFuelLevel(): Flow<Float>

    /** Engine coolant temperature in Celsius */
    fun getCoolantTemp(): Flow<Int>
}
