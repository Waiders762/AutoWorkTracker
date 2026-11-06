package com.autoworktracker.data.backup

import android.content.Context
import android.net.Uri
import com.autoworktracker.domain.util.AppText
import com.autoworktracker.domain.repository.SettingsRepository
import com.autoworktracker.domain.repository.ShiftRepository
import com.autoworktracker.location.MonitoringMaintenance
import com.autoworktracker.startup.StartupState

class BackupRestorer(
    private val context: Context,
    private val backupManager: BackupManager,
    private val shiftRepository: ShiftRepository,
    private val settingsRepository: SettingsRepository,
    private val maintenance: MonitoringMaintenance,
    private val startupState: StartupState,
) {
    suspend fun restore(bytes: ByteArray): Int {
        val language = settingsRepository.current().language
        val parsed = backupManager.parse(bytes, language)
        shiftRepository.replaceAll(parsed.shifts, mirror = false)
        if (parsed.settings != null) {
            settingsRepository.applyRestoredSettings(parsed.settings, mirror = false)
        }
        settingsRepository.setInitialized(true)
        backupManager.mirror()
        maintenance.ensureMonitoring()
        startupState.setOfferRestore(false)
        return parsed.shifts.size
    }

    suspend fun restore(uri: Uri): Int {
        val language = settingsRepository.current().language
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException(AppText.of(language).fileDidNotOpen())
        return restore(bytes)
    }
}
