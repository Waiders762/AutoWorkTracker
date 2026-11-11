package com.autoworktracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.autoworktracker.data.backup.StoredRule
import com.autoworktracker.data.backup.toDomain
import com.autoworktracker.data.backup.toStored
import com.autoworktracker.domain.model.AppLanguage
import com.autoworktracker.domain.model.AppSettings
import com.autoworktracker.domain.model.PresenceState
import com.autoworktracker.domain.model.RestoredSettings
import com.autoworktracker.domain.model.availableCurrencies
import com.autoworktracker.domain.model.ShiftRule
import com.autoworktracker.domain.model.ThemeMode
import com.autoworktracker.domain.model.Workplace
import com.autoworktracker.domain.model.toLocalTime
import com.autoworktracker.domain.model.toMinuteOfDay
import com.autoworktracker.logging.AppLogger
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(context: Context) {
    private val store = context.applicationContext.settingsDataStore
    private val json = Json { ignoreUnknownKeys = true }

    fun observe(): Flow<AppSettings> = store.data.map { preferences -> preferences.toSettings(json) }

    suspend fun current(): AppSettings = store.data.first().toSettings(json)

    suspend fun saveWorkplace(workplace: Workplace) {
        val safe = workplace.sanitized()
        store.edit { prefs ->
            prefs[KEY_WORKPLACE_SET] = true
            prefs[KEY_WORKPLACE_NAME] = safe.name
            prefs[KEY_LATITUDE] = safe.latitude
            prefs[KEY_LONGITUDE] = safe.longitude
            prefs[KEY_RADIUS] = safe.radiusMeters
        }
    }

    suspend fun saveRules(rules: List<ShiftRule>) {
        val payload = json.encodeToString(ListSerializer(StoredRule.serializer()), rules.map { it.toStored() })
        store.edit { prefs -> prefs[KEY_RULES] = payload }
    }

    suspend fun setOverwrite(enabled: Boolean) {
        store.edit { prefs -> prefs[KEY_OVERWRITE] = enabled }
    }

    suspend fun setHourlyRate(rate: Double?) {
        store.edit { prefs ->
            if (rate == null) {
                prefs.remove(KEY_HOURLY_RATE)
            } else {
                prefs[KEY_HOURLY_RATE] = rate.toString()
            }
        }
    }

    suspend fun setCurrency(currency: String) {
        if (currency !in availableCurrencies) return
        store.edit { prefs -> prefs[KEY_CURRENCY] = currency }
    }

    suspend fun setLanguage(language: AppLanguage) {
        store.edit { prefs -> prefs[KEY_LANGUAGE] = language.name }
    }

    suspend fun setTheme(mode: ThemeMode) {
        store.edit { prefs -> prefs[KEY_THEME] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        store.edit { prefs -> prefs[KEY_DYNAMIC] = enabled }
        AppLogger.settings("dynamicColor=$enabled")
    }

    suspend fun setAutoDetectLunchBreak(enabled: Boolean) {
        store.edit { prefs -> prefs[KEY_LUNCH_AUTO] = enabled }
        AppLogger.settings("autoDetectLunchBreak=$enabled")
    }

    suspend fun setLunchWindow(start: LocalTime?, end: LocalTime?) {
        store.edit { prefs ->
            if (start != null) prefs[KEY_LUNCH_START] = start.toMinuteOfDay()
            if (end != null) prefs[KEY_LUNCH_END] = end.toMinuteOfDay()
        }
        AppLogger.settings("lunchWindow start=$start end=$end")
    }

    suspend fun setNoBreakHours(hours: Double) {
        store.edit { prefs -> prefs[KEY_NO_BREAK_HOURS] = hours.toString() }
        AppLogger.settings("noBreakHours=$hours")
    }

    suspend fun setLunchMaxUnlockedMinutes(minutes: Int) {
        store.edit { prefs -> prefs[KEY_LUNCH_UNLOCKED] = minutes.coerceIn(1, 15) }
        AppLogger.settings("lunchMaxUnlockedMinutes=$minutes")
    }

    suspend fun setLunchMinActiveMinutes(minutes: Int) {
        store.edit { prefs -> prefs[KEY_LUNCH_ACTIVE] = minutes.coerceIn(5, 30) }
        AppLogger.settings("lunchMinActiveMinutes=$minutes")
    }

    suspend fun updatePresence(presence: PresenceState) {
        store.edit { prefs ->
            prefs[KEY_ON_SITE] = presence.onSite
            prefs[KEY_DWELL] = presence.dwellConfirmed
            prefs[KEY_SESSION] = presence.sessionConfirmed
            prefs[KEY_NO_BREAK] = presence.noAfternoonBreakToday
            prefs[KEY_BREAK_AUTO] = presence.breakAutoDetected
            prefs.writeEpoch(KEY_FIRST_ENTRY, presence.firstEntryEpochMillis)
            prefs.writeEpoch(KEY_LAST_ENTRY, presence.lastEntryEpochMillis)
        }
        AppLogger.settings(
            "presence onSite=${presence.onSite} noBreak=${presence.noAfternoonBreakToday} auto=${presence.breakAutoDetected}",
        )
    }

    suspend fun setBackupStatus(healthy: Boolean, epochMillis: Long?, error: String?) {
        store.edit { prefs ->
            prefs[KEY_BACKUP_HEALTHY] = healthy
            prefs.writeEpoch(KEY_BACKUP_EPOCH, epochMillis)
            if (error.isNullOrBlank()) {
                prefs.remove(KEY_BACKUP_ERROR)
            } else {
                prefs[KEY_BACKUP_ERROR] = error.take(280)
            }
        }
    }

    suspend fun setInitialized(initialized: Boolean) {
        store.edit { prefs -> prefs[KEY_INITIALIZED] = initialized }
    }

    suspend fun writeRestored(settings: RestoredSettings) {
        val rules = json.encodeToString(
            ListSerializer(StoredRule.serializer()),
            settings.rules.map { it.toStored() },
        )
        store.edit { prefs ->
            prefs[KEY_INITIALIZED] = true
            prefs[KEY_RULES] = rules
            prefs[KEY_OVERWRITE] = settings.overwriteShortWithLong
            prefs[KEY_THEME] = settings.themeMode.name
            prefs[KEY_DYNAMIC] = settings.dynamicColor
            if (settings.hourlyRate == null) {
                prefs.remove(KEY_HOURLY_RATE)
            } else {
                prefs[KEY_HOURLY_RATE] = settings.hourlyRate.toString()
            }
            prefs[KEY_CURRENCY] = settings.currency.takeIf { it in availableCurrencies } ?: "Kč"
            prefs[KEY_LANGUAGE] = settings.language.name
            prefs[KEY_LUNCH_AUTO] = settings.autoDetectLunchBreak
            prefs[KEY_LUNCH_START] = settings.lunchWindowStart.toMinuteOfDay()
            prefs[KEY_LUNCH_END] = settings.lunchWindowEnd.toMinuteOfDay()
            prefs[KEY_NO_BREAK_HOURS] = settings.noBreakHours.toString()
            prefs[KEY_LUNCH_UNLOCKED] = settings.lunchMaxUnlockedMinutes
            prefs[KEY_LUNCH_ACTIVE] = settings.lunchMinActiveMinutes
            val workplace = settings.workplace
            if (workplace == null) {
                prefs[KEY_WORKPLACE_SET] = false
                prefs.remove(KEY_LATITUDE)
                prefs.remove(KEY_LONGITUDE)
                prefs.remove(KEY_RADIUS)
                prefs.remove(KEY_WORKPLACE_NAME)
            } else {
                val safe = workplace.sanitized()
                prefs[KEY_WORKPLACE_SET] = true
                prefs[KEY_WORKPLACE_NAME] = safe.name
                prefs[KEY_LATITUDE] = safe.latitude
                prefs[KEY_LONGITUDE] = safe.longitude
                prefs[KEY_RADIUS] = safe.radiusMeters
            }
            prefs[KEY_ON_SITE] = settings.presence.onSite
            prefs[KEY_DWELL] = settings.presence.dwellConfirmed
            prefs[KEY_SESSION] = settings.presence.sessionConfirmed
            prefs[KEY_NO_BREAK] = settings.presence.noAfternoonBreakToday
            prefs[KEY_BREAK_AUTO] = settings.presence.breakAutoDetected
            prefs.writeEpoch(KEY_FIRST_ENTRY, settings.presence.firstEntryEpochMillis)
            prefs.writeEpoch(KEY_LAST_ENTRY, settings.presence.lastEntryEpochMillis)
        }
    }

    private companion object {
        val KEY_WORKPLACE_SET = booleanPreferencesKey("workplace_set")
        val KEY_WORKPLACE_NAME = stringPreferencesKey("workplace_name")
        val KEY_LATITUDE = doublePreferencesKey("workplace_lat")
        val KEY_LONGITUDE = doublePreferencesKey("workplace_lng")
        val KEY_RADIUS = floatPreferencesKey("workplace_radius")
        val KEY_RULES = stringPreferencesKey("rules_json")
        val KEY_OVERWRITE = booleanPreferencesKey("overwrite_short_with_long")
        val KEY_HOURLY_RATE = stringPreferencesKey("hourly_rate")
        val KEY_CURRENCY = stringPreferencesKey("currency")
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC = booleanPreferencesKey("dynamic_color")
        val KEY_ON_SITE = booleanPreferencesKey("on_site")
        val KEY_DWELL = booleanPreferencesKey("dwell_confirmed")
        val KEY_SESSION = booleanPreferencesKey("session_confirmed")
        val KEY_NO_BREAK = booleanPreferencesKey("no_afternoon_break")
        val KEY_BREAK_AUTO = booleanPreferencesKey("break_auto_detected")
        val KEY_LUNCH_AUTO = booleanPreferencesKey("lunch_auto_detect")
        val KEY_LUNCH_START = intPreferencesKey("lunch_window_start")
        val KEY_LUNCH_END = intPreferencesKey("lunch_window_end")
        val KEY_NO_BREAK_HOURS = stringPreferencesKey("no_break_hours")
        val KEY_LUNCH_UNLOCKED = intPreferencesKey("lunch_max_unlocked_min")
        val KEY_LUNCH_ACTIVE = intPreferencesKey("lunch_min_active_min")
        val KEY_FIRST_ENTRY = longPreferencesKey("first_entry_epoch")
        val KEY_LAST_ENTRY = longPreferencesKey("last_entry_epoch")
        val KEY_INITIALIZED = booleanPreferencesKey("initialized")
        val KEY_BACKUP_HEALTHY = booleanPreferencesKey("backup_healthy")
        val KEY_BACKUP_EPOCH = longPreferencesKey("backup_epoch")
        val KEY_BACKUP_ERROR = stringPreferencesKey("backup_error")
    }
}

private fun Preferences.toSettings(json: Json): AppSettings {
    val workplace = if (this[SettingsKeys.WORKPLACE_SET] == true) {
        val latitude = this[SettingsKeys.LATITUDE]
        val longitude = this[SettingsKeys.LONGITUDE]
        if (latitude != null && longitude != null) {
            Workplace(
                name = this[SettingsKeys.WORKPLACE_NAME] ?: "Работа",
                latitude = latitude,
                longitude = longitude,
                radiusMeters = this[SettingsKeys.RADIUS] ?: Workplace.DEFAULT_RADIUS_METERS,
            ).sanitized()
        } else {
            null
        }
    } else {
        null
    }
    val rules = decodeRules(json, this[SettingsKeys.RULES])
    val rate = this[SettingsKeys.HOURLY_RATE]?.toDoubleOrNull()?.takeIf { it >= 0.0 }
    val currency = this[SettingsKeys.CURRENCY]?.takeIf { it in availableCurrencies } ?: "Kč"
    val language = AppLanguage.fromStored(this[SettingsKeys.LANGUAGE])
    val theme = runCatching { ThemeMode.valueOf(this[SettingsKeys.THEME] ?: ThemeMode.DARK.name) }
        .getOrDefault(ThemeMode.DARK)
    return AppSettings(
        workplace = workplace,
        rules = rules,
        overwriteShortWithLong = this[SettingsKeys.OVERWRITE] ?: true,
        hourlyRate = rate,
        currency = currency,
        language = language,
        autoDetectLunchBreak = this[SettingsKeys.LUNCH_AUTO] ?: true,
        lunchWindowStart = (this[SettingsKeys.LUNCH_START] ?: (14 * 60)).toLocalTime(),
        lunchWindowEnd = (this[SettingsKeys.LUNCH_END] ?: (14 * 60 + 30)).toLocalTime(),
        noBreakHours = this[SettingsKeys.NO_BREAK_HOURS]?.toDoubleOrNull()?.takeIf { it in 0.5..24.0 } ?: 11.5,
        lunchMaxUnlockedMinutes = (this[SettingsKeys.LUNCH_UNLOCKED] ?: 4).coerceIn(1, 15),
        lunchMinActiveMinutes = (this[SettingsKeys.LUNCH_ACTIVE] ?: 18).coerceIn(5, 30),
        presence = PresenceState(
            onSite = this[SettingsKeys.ON_SITE] ?: false,
            firstEntryEpochMillis = this.readEpoch(SettingsKeys.FIRST_ENTRY),
            lastEntryEpochMillis = this.readEpoch(SettingsKeys.LAST_ENTRY),
            dwellConfirmed = this[SettingsKeys.DWELL] ?: false,
            sessionConfirmed = this[SettingsKeys.SESSION] ?: false,
            noAfternoonBreakToday = this[SettingsKeys.NO_BREAK] ?: false,
            breakAutoDetected = this[SettingsKeys.BREAK_AUTO] ?: false,
        ),
        themeMode = theme,
        dynamicColor = this[SettingsKeys.DYNAMIC] ?: false,
        initialized = this[SettingsKeys.INITIALIZED] ?: false,
        lastBackupEpochMillis = this.readEpoch(SettingsKeys.BACKUP_EPOCH),
        lastBackupError = this[SettingsKeys.BACKUP_ERROR],
        backupHealthy = this[SettingsKeys.BACKUP_HEALTHY] ?: false,
    )
}

private fun decodeRules(json: Json, raw: String?): List<ShiftRule> {
    if (raw.isNullOrBlank()) return ShiftRule.defaults()
    return runCatching {
        json.decodeFromString(ListSerializer(StoredRule.serializer()), raw).map { it.toDomain() }
    }.getOrDefault(ShiftRule.defaults())
}

private fun MutablePreferences.writeEpoch(key: Preferences.Key<Long>, value: Long?) {
    if (value == null) remove(key) else this[key] = value
}

private fun Preferences.readEpoch(key: Preferences.Key<Long>): Long? = this[key]?.takeIf { it > 0L }

/**
 * Keys are referenced from the file-level mapper. They mirror the private keys inside
 * [SettingsDataStore] so decoding stays next to the preference names.
 */
private object SettingsKeys {
    val WORKPLACE_SET = booleanPreferencesKey("workplace_set")
    val WORKPLACE_NAME = stringPreferencesKey("workplace_name")
    val LATITUDE = doublePreferencesKey("workplace_lat")
    val LONGITUDE = doublePreferencesKey("workplace_lng")
    val RADIUS = floatPreferencesKey("workplace_radius")
    val RULES = stringPreferencesKey("rules_json")
    val OVERWRITE = booleanPreferencesKey("overwrite_short_with_long")
    val HOURLY_RATE = stringPreferencesKey("hourly_rate")
    val CURRENCY = stringPreferencesKey("currency")
    val LANGUAGE = stringPreferencesKey("language")
    val THEME = stringPreferencesKey("theme_mode")
    val DYNAMIC = booleanPreferencesKey("dynamic_color")
    val ON_SITE = booleanPreferencesKey("on_site")
    val DWELL = booleanPreferencesKey("dwell_confirmed")
    val SESSION = booleanPreferencesKey("session_confirmed")
    val NO_BREAK = booleanPreferencesKey("no_afternoon_break")
    val BREAK_AUTO = booleanPreferencesKey("break_auto_detected")
    val LUNCH_AUTO = booleanPreferencesKey("lunch_auto_detect")
    val LUNCH_START = intPreferencesKey("lunch_window_start")
    val LUNCH_END = intPreferencesKey("lunch_window_end")
    val NO_BREAK_HOURS = stringPreferencesKey("no_break_hours")
    val LUNCH_UNLOCKED = intPreferencesKey("lunch_max_unlocked_min")
    val LUNCH_ACTIVE = intPreferencesKey("lunch_min_active_min")
    val FIRST_ENTRY = longPreferencesKey("first_entry_epoch")
    val LAST_ENTRY = longPreferencesKey("last_entry_epoch")
    val INITIALIZED = booleanPreferencesKey("initialized")
    val BACKUP_HEALTHY = booleanPreferencesKey("backup_healthy")
    val BACKUP_EPOCH = longPreferencesKey("backup_epoch")
    val BACKUP_ERROR = stringPreferencesKey("backup_error")
}
