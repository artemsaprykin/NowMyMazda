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
    val drivingScore: Int, // i-DM driving score 0-100
    val startAddress: String,
    val endAddress: String,
    val tripTag: String = "Commute"
)
