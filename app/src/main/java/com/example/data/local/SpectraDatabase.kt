package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AuditReportEntity::class,
        InterceptedSignalEntity::class,
        ThreatSignatureEntity::class,
        ThreatAlertEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SpectraDatabase : RoomDatabase() {
    abstract fun spectraDao(): SpectraDao

    companion object {
        @Volatile
        private var INSTANCE: SpectraDatabase? = null

        fun getInstance(context: Context): SpectraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpectraDatabase::class.java,
                    "spectra_hunter_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
