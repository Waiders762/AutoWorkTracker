package com.autoworktracker.data.backup

import com.autoworktracker.domain.model.AppLanguage
import com.autoworktracker.domain.model.PresenceState
import com.autoworktracker.domain.model.RestoredSettings
import com.autoworktracker.domain.model.Shift
import com.autoworktracker.domain.model.ShiftRule
import com.autoworktracker.domain.model.ShiftSource
import com.autoworktracker.domain.model.ThemeMode
import com.autoworktracker.domain.model.Workplace
import com.autoworktracker.domain.model.availableCurrencies
import com.autoworktracker.domain.model.toLocalTime
import com.autoworktracker.domain.model.toMinuteOfDay
import java.time.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class StoredRule(
    val id: String,
    val name: String,
    val startMinutes: Int,
    val endMinutes: Int,
    val hours: Double,
    val enabled: Boolean = true,
    val alarmBackupEnabled: Boolean = false,
)

@Serializable
data class StoredShift(
    val dateEpochDay: Long,
    val hours: Double,
    val entryMinutes: Int? = null,
    val exitMinutes: Int? = null,
    val source: String,
    val note: String = "",
    val updatedAtEpochMillis: Long,
)

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val exportedAtEpochMillis: Long,
    val workplaceName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: Float? = null,
    val rules: List<StoredRule>? = null,
    val overwriteShortWithLong: Boolean = true,
    val hourlyRate: Double? = null,
    val currency: String = "Kč",
    val language: String = AppLanguage.RU.name,
    val themeMode: String = ThemeMode.DARK.name,
    val dynamicColor: Boolean = false,
    val onSite: Boolean = false,
    val firstEntryEpochMillis: Long? = null,
    val lastEntryEpochMillis: Long? = null,
    val dwellConfirmed: Boolean = false,
    val sessionConfirmed: Boolean = false,
    val shifts: List<StoredShift> = emptyList(),
)

fun ShiftRule.toStored(): StoredRule {
    return StoredRule(
        id = id,
        name = name,
        startMinutes = windowStart.toMinuteOfDay(),
        endMinutes = windowEnd.toMinuteOfDay(),
        hours = hours,
        enabled = enabled,
        alarmBackupEnabled = alarmBackupEnabled,
    )
}

fun StoredRule.toDomain(): ShiftRule {
    return ShiftRule(
        id = id,
        name = name.ifBlank { "Смена" },
        windowStart = startMinutes.toLocalTime(),
        windowEnd = endMinutes.toLocalTime(),
        hours = hours,
        enabled = enabled,
        alarmBackupEnabled = alarmBackupEnabled,
    )
}

fun Shift.toStored(): StoredShift {
    return StoredShift(
        dateEpochDay = date.toEpochDay(),
        hours = hours,
        entryMinutes = entryTime?.toMinuteOfDay(),
        exitMinutes = exitTime?.toMinuteOfDay(),
        source = source.name,
        note = note,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )
}

fun StoredShift.toDomain(): Shift? {
    if (hours <= 0.0 || hours > 24.0) return null
    val date = runCatching { LocalDate.ofEpochDay(dateEpochDay) }.getOrNull() ?: return null
    val source = runCatching { ShiftSource.valueOf(source) }.getOrDefault(ShiftSource.RESTORED)
    return Shift(
        date = date,
        hours = hours,
        entryTime = entryMinutes?.toLocalTime(),
        exitTime = exitMinutes?.toLocalTime(),
        source = source,
        note = note,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )
}

fun BackupPayload.toRestoredSettings(): RestoredSettings {
    val workplace = if (latitude != null && longitude != null) {
        Workplace(
            name = workplaceName ?: "Работа",
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters ?: Workplace.DEFAULT_RADIUS_METERS,
        ).sanitized()
    } else {
        null
    }
    val theme = runCatching { ThemeMode.valueOf(themeMode) }.getOrDefault(ThemeMode.DARK)
    return RestoredSettings(
        workplace = workplace,
        rules = rules?.map { it.toDomain() } ?: ShiftRule.defaults(),
        overwriteShortWithLong = overwriteShortWithLong,
        hourlyRate = hourlyRate?.takeIf { it >= 0.0 },
        themeMode = theme,
        dynamicColor = dynamicColor,
        currency = currency.takeIf { it in availableCurrencies } ?: "Kč",
        language = AppLanguage.fromStored(language),
        presence = PresenceState(
            onSite = onSite,
            firstEntryEpochMillis = firstEntryEpochMillis,
            lastEntryEpochMillis = lastEntryEpochMillis,
            dwellConfirmed = dwellConfirmed,
            sessionConfirmed = sessionConfirmed,
        ),
    )
}
