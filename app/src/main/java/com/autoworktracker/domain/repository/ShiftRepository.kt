package com.autoworktracker.domain.repository

import com.autoworktracker.domain.model.Shift
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

interface ShiftRepository {
    fun observeAll(): Flow<List<Shift>>
    fun observeMonth(yearMonth: YearMonth): Flow<List<Shift>>
    suspend fun getAll(): List<Shift>
    suspend fun getByDate(date: LocalDate): Shift?
    suspend fun count(): Int
    suspend fun upsert(shift: Shift)
    suspend fun deleteByDate(date: LocalDate)
    suspend fun replaceDate(previous: LocalDate?, shift: Shift)
    suspend fun replaceAll(shifts: List<Shift>, mirror: Boolean = true)
}
