package com.autoworktracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ShiftEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shiftDao(): ShiftDao

    companion object {
        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "autowork.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}
