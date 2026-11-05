package com.autoworktracker.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shifts",
    indices = [Index(value = ["dateEpochDay"], unique = true)],
)
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val hours: Double,
    val entryMinutes: Int?,
    val exitMinutes: Int?,
    val source: String,
    val note: String,
    val updatedAtEpochMillis: Long,
)
