package com.autoworktracker.domain.repository

import com.autoworktracker.domain.model.AppLanguage
import com.autoworktracker.domain.model.AppSettings
import com.autoworktracker.domain.model.PresenceState
import com.autoworktracker.domain.model.RestoredSettings
import com.autoworktracker.domain.model.ShiftRule
import com.autoworktracker.domain.model.ThemeMode
import com.autoworktracker.domain.model.Workplace
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observe(): Flow<AppSettings>
    suspend fun current(): AppSettings
    suspend fun saveWorkplace(workplace: Workplace)
    suspend fun saveRules(rules: List<ShiftRule>)
    suspend fun setOverwrite(enabled: Boolean)
    suspend fun setHourlyRate(rate: Double?)
    suspend fun setCurrency(currency: String)
    suspend fun setLanguage(language: AppLanguage)
    suspend fun setTheme(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun updatePresence(presence: PresenceState)
    suspend fun setInitialized(initialized: Boolean)
    suspend fun normalizePresence()
    suspend fun applyRestoredSettings(settings: RestoredSettings, mirror: Boolean = true)
}
