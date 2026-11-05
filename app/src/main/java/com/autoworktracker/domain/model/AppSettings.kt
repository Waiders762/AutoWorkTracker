package com.autoworktracker.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class PresenceState(
    val onSite: Boolean = false,
    val firstEntryEpochMillis: Long? = null,
    val lastEntryEpochMillis: Long? = null,
    val dwellConfirmed: Boolean = false,
    val sessionConfirmed: Boolean = false,
) {
    fun isForDay(day: LocalDate, zone: ZoneId): Boolean {
        val anchor = firstEntryEpochMillis ?: lastEntryEpochMillis ?: return false
        return Instant.ofEpochMilli(anchor).atZone(zone).toLocalDate() == day
    }
}

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM,
}

enum class AppLanguage {
    RU,
    UK,
    CS,
    EN,
    ;

    val chip: String
        get() = when (this) {
            RU -> "RU"
            UK -> "UA"
            CS -> "CZ"
            EN -> "EN"
        }

    val acceptLanguage: String
        get() = when (this) {
            RU -> "ru"
            UK -> "uk"
            CS -> "cs"
            EN -> "en"
        }

    companion object {
        fun fromStored(raw: String?): AppLanguage {
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: RU
        }
    }
}

val availableCurrencies: List<String> = listOf("Kč", "€", "$", "₴", "zł")

data class AppSettings(
    val workplace: Workplace? = null,
    val rules: List<ShiftRule> = ShiftRule.defaults(),
    val overwriteShortWithLong: Boolean = true,
    val hourlyRate: Double? = null,
    val currency: String = "Kč",
    val language: AppLanguage = AppLanguage.RU,
    val presence: PresenceState = PresenceState(),
    val themeMode: ThemeMode = ThemeMode.DARK,
    val dynamicColor: Boolean = false,
    val initialized: Boolean = false,
    val lastBackupEpochMillis: Long? = null,
    val lastBackupError: String? = null,
    val backupHealthy: Boolean = false,
)

data class RestoredSettings(
    val workplace: Workplace?,
    val rules: List<ShiftRule>,
    val overwriteShortWithLong: Boolean,
    val hourlyRate: Double?,
    val themeMode: ThemeMode,
    val dynamicColor: Boolean,
    val presence: PresenceState,
    val currency: String = "Kč",
    val language: AppLanguage = AppLanguage.RU,
)

object BackupLocations {
    const val DIRECTORY = "AutoWorkTracker"
    const val TEXT_FILE = "Отработанные_часы.txt"
    const val JSON_FILE = "autowork_backup.json"
    const val TEXT_PATH = "/storage/emulated/0/Documents/AutoWorkTracker/Отработанные_часы.txt"
    const val JSON_PATH = "/storage/emulated/0/Documents/AutoWorkTracker/autowork_backup.json"
}
