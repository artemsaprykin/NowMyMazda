package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "maintenance_reminders")
data class MaintenanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vin: String,
    val title: String,
    val description: String,
    val dueOdometerMiles: Int,
    val isCompleted: Boolean = false,
    val priority: String = "Medium", // High, Medium, Low
    val category: String = "Engine" // Engine, Tires, Brakes, Battery, Filters
)
