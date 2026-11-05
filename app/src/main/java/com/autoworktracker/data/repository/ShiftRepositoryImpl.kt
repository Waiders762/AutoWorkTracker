package com.autoworktracker.data.repository

import androidx.room.withTransaction
import com.autoworktracker.data.backup.BackupManager
import com.autoworktracker.data.local.AppDatabase
import com.autoworktracker.data.local.ShiftDao
import com.autoworktracker.data.local.toDomain
import com.autoworktracker.data.local.toEntity
import com.autoworktracker.domain.model.Shift
import com.autoworktracker.domain.repository.ShiftRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShiftRepositoryImpl(
    private val database: AppDatabase,
    private val shiftDao: ShiftDao,
    private val backupManager: BackupManager,
) : ShiftRepository {
    override fun observeAll(): Flow<List<Shift>> {
        return shiftDao.observeAll().map { rows -> rows.map { it.toDomain() } }
    }

    override fun observeMonth(yearMonth: YearMonth): Flow<List<Shift>> {
        val start = yearMonth.atDay(1).toEpochDay()
        val end = yearMonth.atEndOfMonth().toEpochDay()
        return shiftDao.observeBetween(start, end).map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun getAll(): List<Shift> = shiftDao.getAll().map { it.toDomain() }

    override suspend fun getByDate(date: LocalDate): Shift? = shiftDao.getByDay(date.toEpochDay())?.toDomain()

    override suspend fun count(): Int = shiftDao.count()

    override suspend fun upsert(shift: Shift) {
        database.withTransaction { writeShift(shift) }
        backupManager.mirror()
    }

    override suspend fun deleteByDate(date: LocalDate) {
        shiftDao.deleteByDay(date.toEpochDay())
        backupManager.mirror()
    }

    override suspend fun replaceDate(previous: LocalDate?, shift: Shift) {
        database.withTransaction {
            if (previous != null && previous != shift.date) {
                shiftDao.deleteByDay(previous.toEpochDay())
            }
            writeShift(shift)
        }
        backupManager.mirror()
    }

    override suspend fun replaceAll(shifts: List<Shift>, mirror: Boolean) {
        database.withTransaction {
            shiftDao.deleteAll()
            if (shifts.isNotEmpty()) {
                shiftDao.insertAll(shifts.map { it.toEntity().copy(id = 0) })
            }
        }
        if (mirror) backupManager.mirror()
    }

    private suspend fun writeShift(shift: Shift) {
        val existing = shiftDao.getByDay(shift.date.toEpochDay())
        val entity = shift.toEntity().copy(id = existing?.id ?: 0)
        if (existing == null) {
            shiftDao.insert(entity)
        } else {
            shiftDao.update(entity)
        }
    }
}
