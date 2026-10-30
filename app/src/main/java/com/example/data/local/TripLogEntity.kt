package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_logs")
data class TripLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vin: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val distanceMiles: Double,
    val durationMinutes: Int,
    val avgMpg: Double,
    val avgSpeedMph: Double,
    val maxSpeedMph: Double = 65.0,
    val drivingScore: Int, // Mazda i-DM score 0-100
    val startAddress: String,
    val endAddress: String,
    val routeSummary: String = "Direct Highway & Arterial Route",
    val waypointsJson: String = "", // JSON list of route waypoints
    val fuelConsumedGal: Double = 0.8,
    val tripTag: String = "Commute"
)
