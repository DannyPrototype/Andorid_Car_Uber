package com.dannyprototype.carradio.ui.state

import com.google.android.gms.maps.model.LatLng

data class MapUiState(
    val currentPosition: LatLng? = null,
    val routePoints: List<LatLng> = emptyList(),
    val speedKmh: Int = 0,
    val isMoving: Boolean = false,
    val bearing: Float = 0f
)
