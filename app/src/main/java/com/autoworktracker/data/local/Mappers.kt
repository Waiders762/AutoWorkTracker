package com.autoworktracker.data.local

import com.autoworktracker.domain.model.Shift
import com.autoworktracker.domain.model.ShiftSource
import com.autoworktracker.domain.model.toLocalTime
import com.autoworktracker.domain.model.toMinuteOfDay
import java.time.LocalDate

fun ShiftEntity.toDomain(): Shift {
    return Shift(
        id = id,
        date = LocalDate.ofEpochDay(dateEpochDay),
        hours = hours,
        entryTime = entryMinutes?.toLocalTime(),
        exitTime = exitMinutes?.toLocalTime(),
        source = runCatching { ShiftSource.valueOf(source) }.getOrDefault(ShiftSource.MANUAL),
        note = note,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )
}

fun Shift.toEntity(): ShiftEntity {
    return ShiftEntity(
        id = id,
        dateEpochDay = date.toEpochDay(),
        hours = hours,
        entryMinutes = entryTime?.toMinuteOfDay(),
        exitMinutes = exitTime?.toMinuteOfDay(),
        source = source.name,
        note = note,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )
}
