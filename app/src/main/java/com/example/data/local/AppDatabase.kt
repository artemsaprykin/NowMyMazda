package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TripLogEntity::class, MaintenanceEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripLogDao(): TripLogDao
    abstract fun maintenanceDao(): MaintenanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE trip_logs ADD COLUMN maxSpeedMph REAL NOT NULL DEFAULT 65.0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE trip_logs ADD COLUMN routeSummary TEXT NOT NULL DEFAULT 'Direct Route'")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE trip_logs ADD COLUMN waypointsJson TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE trip_logs ADD COLUMN fuelConsumedGal REAL NOT NULL DEFAULT 0.8")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE trip_logs ADD COLUMN tripTag TEXT NOT NULL DEFAULT 'Commute'")
                } catch (_: Exception) {}
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mazda_connect_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
