package com.dannyprototype.carradio.ui.state

data class GeoPoint(val latitude: Double, val longitude: Double)

data class MapUiState(
    val currentPosition: GeoPoint? = null,
    val routePoints: List<GeoPoint> = emptyList(),
    val speedKmh: Int = 0,
    val isMoving: Boolean = false,
    val bearing: Float = 0f
)
