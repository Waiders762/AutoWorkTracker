package com.autoworktracker.ui.settings

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autoworktracker.AutoWorkTrackerApp
import com.autoworktracker.domain.model.AppLanguage
import com.autoworktracker.domain.model.AppSettings
import com.autoworktracker.domain.model.ShiftRule
import com.autoworktracker.domain.model.ThemeMode
import com.autoworktracker.domain.model.Workplace
import com.autoworktracker.domain.util.AppText
import com.autoworktracker.domain.util.WorkReportFormatter
import com.autoworktracker.notification.NotificationHelper
import com.autoworktracker.ui.common.UserEvent
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val permissions: PermissionSnapshot = PermissionSnapshot(),
    val locating: Boolean = false,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as AutoWorkTrackerApp).container
    private val permissionTick = MutableStateFlow(0)
    private val locating = MutableStateFlow(false)
    private val eventsFlow = MutableSharedFlow<UserEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UserEvent> = eventsFlow.asSharedFlow()

    val state: StateFlow<SettingsUiState> = combine(
        container.settingsRepository.observe(),
        permissionTick,
        locating,
    ) { settings, _, busy ->
        SettingsUiState(
            settings = settings,
            permissions = PermissionSnapshot.capture(getApplication()),
            locating = busy,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun refreshPermissions() {
        permissionTick.value += 1
    }

    fun syncMonitoring() {
        refreshPermissions()
        viewModelScope.launch { container.maintenance.ensureMonitoring() }
    }

    fun deviceFixes() = container.locationHelper.fixes()

    fun previewCurrentLocation(onReady: (latitude: Double?, longitude: Double?) -> Unit) {
        viewModelScope.launch {
            locating.value = true
            val location = container.locationHelper.currentPrecise()
            locating.value = false
            if (location == null) {
                eventsFlow.emit(UserEvent.Snackbar(text().gpsNoResponse))
                onReady(null, null)
            } else {
                onReady(location.latitude, location.longitude)
            }
        }
    }

    fun saveMapPoint(
        latitude: Double,
        longitude: Double,
        radiusMeters: Float,
        onSaved: () -> Unit,
    ) {
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            eventsFlow.tryEmit(UserEvent.Snackbar(text().coordinatesInvalid))
            return
        }
        viewModelScope.launch {
            val labels = text()
            val current = container.settingsRepository.current().workplace
            saveWorkplace(
                Workplace(
                    name = current?.name ?: labels.defaultWorkplaceName,
                    latitude = latitude,
                    longitude = longitude,
                    radiusMeters = radiusMeters,
                ),
            )
            eventsFlow.emit(UserEvent.Snackbar(labels.pointSaved))
            onSaved()
        }
    }

    fun saveManualWorkplace(name: String, latitudeRaw: String, longitudeRaw: String, radius: Float) {
        val latitude = latitudeRaw.replace(',', '.').toDoubleOrNull()
        val longitude = longitudeRaw.replace(',', '.').toDoubleOrNull()
        if (latitude == null || longitude == null || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
            eventsFlow.tryEmit(UserEvent.Snackbar(text().coordinateBounds))
            return
        }
        viewModelScope.launch {
            val labels = text()
            saveWorkplace(
                Workplace(
                    name = name.ifBlank { labels.defaultWorkplaceName },
                    latitude = latitude,
                    longitude = longitude,
                    radiusMeters = radius,
                ),
            )
            eventsFlow.emit(UserEvent.Snackbar(labels.pointSaved))
        }
    }

    fun updateRadius(radius: Float) {
        viewModelScope.launch {
            val current = container.settingsRepository.current().workplace ?: return@launch
            saveWorkplace(current.copy(radiusMeters = radius))
        }
    }

    fun setOverwrite(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setOverwrite(enabled) }
    }

    fun setHourlyRate(raw: String) {
        val cleaned = raw.trim().replace(" ", "").replace(',', '.')
        if (cleaned.isEmpty()) {
            viewModelScope.launch { container.settingsRepository.setHourlyRate(null) }
            return
        }
        val rate = cleaned.toDoubleOrNull()
        if (rate == null || rate < 0.0) {
            eventsFlow.tryEmit(UserEvent.Snackbar(text().rateNotNumber))
            return
        }
        viewModelScope.launch { container.settingsRepository.setHourlyRate(rate) }
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch { container.settingsRepository.setCurrency(currency) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            container.settingsRepository.setLanguage(language)
            NotificationHelper.createChannels(getApplication())
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { container.settingsRepository.setTheme(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setDynamicColor(enabled) }
    }

    fun setRuleEnabled(id: String, enabled: Boolean) = updateRule(id) { it.copy(enabled = enabled) }

    fun setAlarmBackup(id: String, enabled: Boolean) = updateRule(id) { it.copy(alarmBackupEnabled = enabled) }

    fun setRuleName(id: String, name: String) = updateRule(id) {
        it.copy(name = name.ifBlank { text().defaultShiftName }.take(40))
    }

    fun setRuleHours(id: String, raw: String) {
        val hours = raw.replace(',', '.').toDoubleOrNull()
        if (hours == null || hours < 0.5 || hours > 24.0) {
            eventsFlow.tryEmit(UserEvent.Snackbar(text().ruleHoursOutOfRange))
            return
        }
        updateRule(id) { it.copy(hours = hours) }
    }

    fun setRuleWindow(id: String, start: LocalTime?, end: LocalTime?) {
        updateRule(id) { rule ->
            rule.copy(
                windowStart = start ?: rule.windowStart,
                windowEnd = end ?: rule.windowEnd,
            )
        }
    }

    fun addRule() {
        viewModelScope.launch {
            val rules = container.settingsRepository.current().rules + ShiftRule(
                id = UUID.randomUUID().toString(),
                name = text().newRuleName,
                windowStart = LocalTime.of(12, 0),
                windowEnd = LocalTime.of(13, 0),
                hours = 8.0,
            )
            container.settingsRepository.saveRules(rules)
            container.maintenance.rescheduleAlarms()
        }
    }

    fun deleteRule(id: String) {
        viewModelScope.launch {
            val rules = container.settingsRepository.current().rules.filterNot { it.id == id }
            container.settingsRepository.saveRules(rules)
            container.maintenance.rescheduleAlarms()
        }
    }

    fun copyCurrentMonth() {
        viewModelScope.launch {
            val language = container.settingsRepository.current().language
            val labels = AppText.of(language)
            val month = YearMonth.now()
            val report = WorkReportFormatter.monthReport(container.shiftRepository.getAll(), month, language)
            val clipboard = getApplication<Application>().getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(ClipData.newPlainText(labels.clipboardTitle, report))
            eventsFlow.emit(UserEvent.Snackbar(labels.currentMonthCopied))
        }
    }

    fun shareFile() {
        viewModelScope.launch {
            val language = container.settingsRepository.current().language
            val all = container.shiftRepository.getAll()
            val month = YearMonth.now()
            eventsFlow.emit(
                UserEvent.Share(
                    message = WorkReportFormatter.monthReport(all, month, language),
                    attachment = WorkReportFormatter.fullReport(all, LocalDateTime.now(), language),
                ),
            )
        }
    }

    fun restoreFromUri(uri: Uri) {
        viewModelScope.launch {
            val labels = AppText.of(container.settingsRepository.current().language)
            try {
                val count = container.backupRestorer.restore(uri)
                eventsFlow.emit(UserEvent.Snackbar(labels.restoredCount(count)))
            } catch (error: Exception) {
                eventsFlow.emit(UserEvent.Snackbar(error.message ?: labels.backupUnreadable))
            }
        }
    }

    private fun text(): AppText = AppText.of(state.value.settings.language)

    private fun updateRule(id: String, transform: (ShiftRule) -> ShiftRule) {
        viewModelScope.launch {
            val rules = container.settingsRepository.current().rules.map { rule ->
                if (rule.id == id) transform(rule) else rule
            }
            container.settingsRepository.saveRules(rules)
            container.maintenance.rescheduleAlarms()
        }
    }

    private suspend fun saveWorkplace(workplace: Workplace) {
        container.settingsRepository.saveWorkplace(workplace.sanitized())
        container.maintenance.ensureGeofence()
    }
}
