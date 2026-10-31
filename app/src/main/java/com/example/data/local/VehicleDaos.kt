package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripLogDao {
    @Query("SELECT * FROM trip_logs ORDER BY startTimeMillis DESC")
    fun getAllTrips(): Flow<List<TripLogEntity>>

    @Query("SELECT * FROM trip_logs WHERE vin = :vin ORDER BY startTimeMillis DESC")
    fun getTripsForVin(vin: String): Flow<List<TripLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripLogEntity): Long

    @Query("DELETE FROM trip_logs WHERE id = :tripId")
    suspend fun deleteTrip(tripId: Long)

    @Query("DELETE FROM trip_logs WHERE vin = :vin")
    suspend fun clearAllTrips(vin: String)
}

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance_reminders WHERE vin = :vin ORDER BY isCompleted ASC, dueOdometerMiles ASC")
    fun getRemindersForVin(vin: String): Flow<List<MaintenanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: MaintenanceEntity): Long

    @Update
    suspend fun updateReminder(reminder: MaintenanceEntity)

    @Query("UPDATE maintenance_reminders SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM maintenance_reminders WHERE id = :id")
    suspend fun deleteReminder(id: Long)
}
