package com.dannyprototype.carradio.model

import com.dannyprototype.carradio.R

enum class TripMode(
    val displayName: String,
    val iconEmoji: String,
    val bgDrawableRes: Int,
    val colorRes: Int
) {
    PERSONAL(
        displayName = "Personal",
        iconEmoji = "\uD83D\uDE98",
        bgDrawableRes = R.drawable.bg_mode_personal,
        colorRes = R.color.mode_personal
    ),
    UBER_WITH_PASSENGER(
        displayName = "Uber con Pasajero",
        iconEmoji = "\uD83D\uDFE2",
        bgDrawableRes = R.drawable.bg_mode_uber_passenger,
        colorRes = R.color.mode_uber_passenger
    ),
    UBER_WITHOUT_PASSENGER(
        displayName = "Uber sin Pasajero",
        iconEmoji = "\uD83D\uDFE1",
        bgDrawableRes = R.drawable.bg_mode_uber_empty,
        colorRes = R.color.mode_uber_empty
    );

    fun next(): TripMode = entries[(ordinal + 1) % entries.size]
}
