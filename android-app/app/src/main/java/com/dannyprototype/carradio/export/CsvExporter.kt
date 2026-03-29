package com.dannyprototype.carradio.export

import android.content.Context
import com.dannyprototype.carradio.data.entity.TripEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    /**
     * Export trips to a CSV file in app cache.
     * Returns the file path on success, null on failure.
     */
    fun export(context: Context, trips: List<TripEntity>): String? {
        if (trips.isEmpty()) return null

        val headers = listOf(
            "ID", "Fecha", "Modo", "Distancia_km", "Duracion_seg",
            "Tiempo_Detenido_seg", "Vel_Promedio_kmh", "Costo_por_km",
            "Costo_Viaje", "Ingreso", "Ganancia", "Km_Dia_Acumulado"
        )

        val sb = StringBuilder()
        sb.appendLine(headers.joinToString(","))

        for (trip in trips) {
            val row = listOf(
                trip.id.toString(),
                "\"${displayFormat.format(Date(trip.startTime))}\"",
                "\"${trip.mode}\"",
                String.format("%.3f", trip.distanceKm),
                trip.durationSeconds.toString(),
                trip.stoppedTimeSeconds.toString(),
                String.format("%.1f", trip.avgSpeedKmh),
                String.format("%.4f", trip.fuelCostPerKm),
                String.format("%.2f", trip.tripCost),
                String.format("%.2f", trip.income),
                String.format("%.2f", trip.profit),
                String.format("%.2f", trip.dayKmAccumulated)
            )
            sb.appendLine(row.joinToString(","))
        }

        val dir = File(context.cacheDir, "exports")
        dir.mkdirs()
        val fileName = "viajes_${dateFormat.format(Date())}.csv"
        val file = File(dir, fileName)
        file.writeText(sb.toString())
        return file.absolutePath
    }
}
