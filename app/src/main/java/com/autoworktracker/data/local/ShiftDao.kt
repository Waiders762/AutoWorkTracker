package com.autoworktracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts ORDER BY dateEpochDay DESC")
    fun observeAll(): Flow<List<ShiftEntity>>

    @Query(
        "SELECT * FROM shifts WHERE dateEpochDay BETWEEN :start AND :endInclusive ORDER BY dateEpochDay DESC",
    )
    fun observeBetween(start: Long, endInclusive: Long): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts ORDER BY dateEpochDay ASC")
    suspend fun getAll(): List<ShiftEntity>

    @Query("SELECT * FROM shifts WHERE dateEpochDay = :day LIMIT 1")
    suspend fun getByDay(day: Long): ShiftEntity?

    @Query("SELECT COUNT(*) FROM shifts")
    suspend fun count(): Int

    @Insert
    suspend fun insert(entity: ShiftEntity): Long

    @Insert
    suspend fun insertAll(entities: List<ShiftEntity>)

    @Update
    suspend fun update(entity: ShiftEntity)

    @Query("DELETE FROM shifts WHERE dateEpochDay = :day")
    suspend fun deleteByDay(day: Long)

    @Query("DELETE FROM shifts")
    suspend fun deleteAll()
}
