package com.autoworktracker.data.repository

import com.autoworktracker.data.backup.BackupManager
import com.autoworktracker.data.preferences.SettingsDataStore
import com.autoworktracker.domain.model.AppLanguage
import com.autoworktracker.domain.model.AppSettings
import com.autoworktracker.domain.model.PresenceState
import com.autoworktracker.domain.model.RestoredSettings
import com.autoworktracker.domain.model.ShiftRule
import com.autoworktracker.domain.model.ThemeMode
import com.autoworktracker.domain.model.Workplace
import com.autoworktracker.domain.repository.SettingsRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val settingsDataStore: SettingsDataStore,
    private val backupManager: BackupManager,
) : SettingsRepository {
    override fun observe(): Flow<AppSettings> = settingsDataStore.observe()

    override suspend fun current(): AppSettings = settingsDataStore.current()

    override suspend fun saveWorkplace(workplace: Workplace) {
        settingsDataStore.saveWorkplace(workplace)
        backupManager.mirror()
    }

    override suspend fun saveRules(rules: List<ShiftRule>) {
        settingsDataStore.saveRules(rules)
        backupManager.mirror()
    }

    override suspend fun setOverwrite(enabled: Boolean) {
        settingsDataStore.setOverwrite(enabled)
        backupManager.mirror()
    }

    override suspend fun setHourlyRate(rate: Double?) {
        settingsDataStore.setHourlyRate(rate)
        backupManager.mirror()
    }

    override suspend fun setCurrency(currency: String) {
        settingsDataStore.setCurrency(currency)
        backupManager.mirror()
    }

    override suspend fun setLanguage(language: AppLanguage) {
        settingsDataStore.setLanguage(language)
        backupManager.mirror()
    }

    override suspend fun setTheme(mode: ThemeMode) {
        settingsDataStore.setTheme(mode)
        backupManager.mirror()
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        settingsDataStore.setDynamicColor(enabled)
        backupManager.mirror()
    }

    override suspend fun setAutoDetectLunchBreak(enabled: Boolean) {
        settingsDataStore.setAutoDetectLunchBreak(enabled)
        backupManager.mirror()
    }

    override suspend fun setLunchWindow(start: LocalTime?, end: LocalTime?) {
        settingsDataStore.setLunchWindow(start, end)
        backupManager.mirror()
    }

    override suspend fun setNoBreakHours(hours: Double) {
        settingsDataStore.setNoBreakHours(hours)
        backupManager.mirror()
    }

    override suspend fun setLunchMaxUnlockedMinutes(minutes: Int) {
        settingsDataStore.setLunchMaxUnlockedMinutes(minutes)
        backupManager.mirror()
    }

    override suspend fun setLunchMinActiveMinutes(minutes: Int) {
        settingsDataStore.setLunchMinActiveMinutes(minutes)
        backupManager.mirror()
    }

    override suspend fun updatePresence(presence: PresenceState) {
        settingsDataStore.updatePresence(presence)
    }

    override suspend fun setInitialized(initialized: Boolean) {
        settingsDataStore.setInitialized(initialized)
    }

    override suspend fun normalizePresence() {
        val settings = settingsDataStore.current()
        val today = LocalDate.now()
        val presence = settings.presence
        if (presence != PresenceState() && !presence.isForDay(today, ZoneId.systemDefault())) {
            settingsDataStore.updatePresence(PresenceState())
        }
    }

    override suspend fun applyRestoredSettings(settings: RestoredSettings, mirror: Boolean) {
        settingsDataStore.writeRestored(settings)
        if (mirror) backupManager.mirror()
    }
}
