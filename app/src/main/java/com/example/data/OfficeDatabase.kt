package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [OfficeDocument::class], version = 1, exportSchema = false)
abstract class OfficeDatabase : RoomDatabase() {
    abstract fun officeDao(): OfficeDao

    companion object {
        @Volatile
        private var INSTANCE: OfficeDatabase? = null

        fun getDatabase(context: Context): OfficeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OfficeDatabase::class.java,
                    "maxoffice_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
