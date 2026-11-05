package com.autoworktracker.startup

import android.util.Log
import com.autoworktracker.data.backup.BackupManager
import com.autoworktracker.domain.repository.SettingsRepository
import com.autoworktracker.domain.repository.ShiftRepository
import com.autoworktracker.location.MonitoringMaintenance
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class StartupState {
    private val mutableMessages = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    val messages: SharedFlow<String> = mutableMessages.asSharedFlow()

    private val mutableOfferRestore = MutableStateFlow(false)
    val offerRestore: StateFlow<Boolean> = mutableOfferRestore.asStateFlow()

    fun post(message: String) {
        mutableMessages.tryEmit(message)
    }

    fun setOfferRestore(offer: Boolean) {
        mutableOfferRestore.value = offer
    }
}

class AppStartup(
    private val shiftRepository: ShiftRepository,
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager,
    private val maintenance: MonitoringMaintenance,
    private val startupState: StartupState,
) {
    suspend fun prepare() {
        settingsRepository.normalizePresence()
        if (shiftRepository.count() == 0) {
            val bytes = backupManager.readBestBackup()
            if (bytes != null) {
                try {
                    val parsed = backupManager.parse(bytes)
                    shiftRepository.replaceAll(parsed.shifts, mirror = false)
                    parsed.settings?.let { settingsRepository.applyRestoredSettings(it, mirror = false) }
                    settingsRepository.setInitialized(true)
                    backupManager.mirror()
                    startupState.post("История восстановлена: ${parsed.shifts.size}")
                    startupState.setOfferRestore(false)
                } catch (error: Exception) {
                    Log.e(TAG, "Automatic restore failed", error)
                    startupState.post("Бэкап найден, но не прочитан. Выберите файл вручную.")
                    startupState.setOfferRestore(true)
                }
            } else {
                startupState.setOfferRestore(true)
                backupManager.mirror()
            }
        }
        maintenance.ensureMonitoring()
    }

    private companion object {
        const val TAG = "AutoWork"
    }
}
