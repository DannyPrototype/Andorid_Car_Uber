package com.dannyprototype.carradio.obd

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub implementation of OBD2Manager.
 * Returns empty/zero values. Replace with real implementation
 * when Bluetooth OBD2 adapter integration is ready.
 */
@Singleton
class OBD2ManagerStub @Inject constructor() : OBD2Manager {

    override suspend fun connect(deviceAddress: String): Boolean {
        // TODO: Implement Bluetooth connection to ELM327
        return false
    }

    override suspend fun disconnect() {
        // No-op
    }

    override fun isConnected(): Boolean = false

    override fun getSpeed(): Flow<Int> = emptyFlow()

    override fun getRPM(): Flow<Int> = emptyFlow()

    override fun getFuelLevel(): Flow<Float> = emptyFlow()

    override fun getCoolantTemp(): Flow<Int> = emptyFlow()
}
