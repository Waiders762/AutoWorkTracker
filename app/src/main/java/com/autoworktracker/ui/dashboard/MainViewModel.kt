package com.autoworktracker.ui.dashboard

import kotlinx.coroutines.ExperimentalCoroutinesApi

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autoworktracker.AutoWorkTrackerApp
import com.autoworktracker.domain.model.AppLanguage
import com.autoworktracker.domain.model.PresenceState
import com.autoworktracker.domain.model.Shift
import com.autoworktracker.domain.model.ShiftSource
import com.autoworktracker.domain.util.AppText
import com.autoworktracker.domain.util.DistanceFormatter
import com.autoworktracker.domain.util.HoursFormatter
import com.autoworktracker.domain.util.RussianDateFormatter
import com.autoworktracker.domain.util.WorkReportFormatter
import com.autoworktracker.location.LocationPermissions
import com.autoworktracker.ui.common.UserEvent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as AutoWorkTrackerApp).container
    private val month = MutableStateFlow(YearMonth.now())
    private val distance = MutableStateFlow<DistanceReading>(DistanceReading.Idle)
    private val editor = MutableStateFlow<EditorState?>(null)
    private val eventsFlow = MutableSharedFlow<UserEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UserEvent> = eventsFlow.asSharedFlow()
    private val startupShown = AtomicBoolean(false)

    private val core = month.flatMapLatest { yearMonth ->
        combine(
            container.shiftRepository.observeMonth(yearMonth),
            container.settingsRepository.observe(),
            container.geofenceManager.registration,
        ) { shifts, settings, geofence ->
            DashboardCore(yearMonth, shifts, settings, geofence)
        }
    }

    private val restoreOffer = combine(
        container.startupState.offerRestore,
        container.shiftRepository.observeAll(),
    ) { offer, shifts ->
        offer && shifts.isEmpty()
    }

    val state: StateFlow<DashboardUiState> = combine(
        core,
        distance,
        editor,
        restoreOffer,
    ) { coreState, distanceReading, editorState, offer ->
        mapState(coreState, distanceReading, editorState, offer)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    init {
        viewModelScope.launch {
            container.startupState.messages.collect { message ->
                if (startupShown.compareAndSet(false, true)) {
                    eventsFlow.emit(UserEvent.Snackbar(message))
                }
            }
        }
        refreshDistance()
    }

    fun shiftMonth(delta: Int) {
        month.value = month.value.plusMonths(delta.toLong())
    }

    fun refreshDistance() {
        viewModelScope.launch {
            val workplace = container.settingsRepository.current().workplace
            if (workplace == null) {
                distance.value = DistanceReading.NoWorkplace
                return@launch
            }
            if (!LocationPermissions.hasForeground(getApplication())) {
                distance.value = DistanceReading.NoPermission
                return@launch
            }
            distance.value = DistanceReading.Loading
            val location = container.locationHelper.currentPrecise()
            val radiusLabel = workplace.radiusMeters.roundToInt()
            if (location == null || !location.hasAccuracy() || location.accuracy > 80f) {
                distance.value = DistanceReading.Approximate(radiusLabel)
                return@launch
            }
            val meters = container.locationHelper.distanceMeters(
                location.latitude,
                location.longitude,
                workplace.latitude,
                workplace.longitude,
            )
            distance.value = DistanceReading.Ready(meters = meters, inside = meters <= workplace.radiusMeters)
        }
    }

    fun openCreate() {
        val longHours = state.value.rules.firstOrNull { it.id == "long" }?.hours ?: 11.0
        editor.value = EditorState(
            originalDate = null,
            date = LocalDate.now(),
            hoursText = HoursFormatter.formatNumber(longHours, state.value.language),
        )
    }

    fun openEdit(shift: Shift) {
        editor.value = EditorState(
            originalDate = shift.date,
            date = shift.date,
            hoursText = HoursFormatter.formatNumber(shift.hours, state.value.language),
        )
    }

    fun dismissEditor() {
        editor.value = null
    }

    fun updateEditorDate(date: LocalDate) {
        editor.value = editor.value?.copy(date = date)
    }

    fun updateEditorHours(text: String) {
        val cleaned = text.filter { it.isDigit() || it == ',' || it == '.' }.take(5)
        editor.value = editor.value?.copy(hoursText = cleaned)
    }

    fun saveEditor() {
        val current = editor.value ?: return
        val hours = current.hoursText.replace(',', '.').toDoubleOrNull()
        val text = AppText.of(state.value.language)
        if (hours == null || hours < 0.5 || hours > 24.0) {
            eventsFlow.tryEmit(UserEvent.Snackbar(text.hoursOutOfRange))
            return
        }
        viewModelScope.launch {
            val original = current.originalDate?.let { container.shiftRepository.getByDate(it) }
            val target = container.shiftRepository.getByDate(current.date)
            val carried = if (current.originalDate == current.date) target else original
            val shift = Shift(
                id = target?.id ?: 0,
                date = current.date,
                hours = hours,
                entryTime = carried?.entryTime,
                exitTime = carried?.exitTime,
                source = ShiftSource.MANUAL,
                note = carried?.note.orEmpty(),
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
            container.shiftRepository.replaceDate(current.originalDate, shift)
            editor.value = null
            eventsFlow.emit(UserEvent.Snackbar(AppText.of(state.value.language).shiftSaved))
        }
    }

    fun deleteShift(shift: Shift) {
        viewModelScope.launch {
            container.shiftRepository.deleteByDate(shift.date)
            if (editor.value?.date == shift.date || editor.value?.originalDate == shift.date) {
                editor.value = null
            }
            eventsFlow.emit(UserEvent.UndoDelete(shift))
        }
    }

    fun undoDelete(shift: Shift) {
        viewModelScope.launch {
            container.shiftRepository.upsert(shift)
        }
    }

    fun copyMonth() {
        viewModelScope.launch {
            val language = container.settingsRepository.current().language
            val text = AppText.of(language)
            val report = WorkReportFormatter.monthReport(container.shiftRepository.getAll(), month.value, language)
            val clipboard = getApplication<Application>().getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(ClipData.newPlainText(text.clipboardTitle, report))
            eventsFlow.emit(UserEvent.Snackbar(text.monthCopied))
        }
    }

    fun shareReport() {
        viewModelScope.launch {
            val language = container.settingsRepository.current().language
            val all = container.shiftRepository.getAll()
            eventsFlow.emit(
                UserEvent.Share(
                    message = WorkReportFormatter.monthReport(all, month.value, language),
                    attachment = WorkReportFormatter.fullReport(all, LocalDateTime.now(), language),
                ),
            )
        }
    }

    fun dismissRestoreOffer() {
        container.startupState.setOfferRestore(false)
    }

    fun restoreFromUri(uri: android.net.Uri) {
        viewModelScope.launch {
            val text = AppText.of(container.settingsRepository.current().language)
            try {
                val count = container.backupRestorer.restore(uri)
                eventsFlow.emit(UserEvent.Snackbar(text.restoredCount(count)))
            } catch (error: Exception) {
                eventsFlow.emit(UserEvent.Snackbar(error.message ?: text.backupUnreadable))
            }
        }
    }

    private fun mapState(
        coreState: DashboardCore,
        distanceReading: DistanceReading,
        editorState: EditorState?,
        offerRestore: Boolean,
    ): DashboardUiState {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val presence = coreState.settings.presence
        val todayPresence = if (presence.isForDay(today, zone)) presence else PresenceState()
        val summary = container.monthSummary.build(
            shifts = coreState.shifts,
            rules = coreState.settings.rules,
            hourlyRate = coreState.settings.hourlyRate,
        )
        val text = AppText.of(coreState.settings.language)
        val entryLabel = todayPresence.firstEntryEpochMillis
            ?.takeIf { todayPresence.onSite }
            ?.let { epoch ->
                val time = Instant.ofEpochMilli(epoch).atZone(zone).toLocalTime()
                text.enteredAt(RussianDateFormatter.formatTime(time))
            }
        return DashboardUiState(
            loading = false,
            yearMonth = coreState.yearMonth,
            monthTitle = RussianDateFormatter.monthTitle(coreState.yearMonth, coreState.settings.language),
            shifts = coreState.shifts.sortedByDescending { it.date },
            summary = summary,
            rules = coreState.settings.rules,
            currency = coreState.settings.currency,
            language = coreState.settings.language,
            statusTitle = statusTitle(coreState.settings.workplace != null, todayPresence, text),
            statusDetail = statusDetail(
                distanceReading,
                text.geofenceStatus(coreState.geofence.status),
                text,
                coreState.settings.language,
            ),
            entryLabel = entryLabel,
            onSite = todayPresence.onSite,
            confirmed = todayPresence.dwellConfirmed || todayPresence.sessionConfirmed,
            distanceLabel = distanceLabel(distanceReading, text, coreState.settings.language),
            workplaceConfigured = coreState.settings.workplace != null,
            geofenceActive = coreState.geofence.active,
            geofenceMessage = text.geofenceStatus(coreState.geofence.status),
            backupHealthy = coreState.settings.backupHealthy,
            backupError = coreState.settings.lastBackupError,
            editor = editorState,
            offerRestore = offerRestore,
        )
    }

    private fun statusTitle(hasWorkplace: Boolean, presence: PresenceState, text: AppText): String {
        return when {
            !hasWorkplace -> text.workplaceMissing
            presence.onSite && (presence.dwellConfirmed || presence.sessionConfirmed) -> text.workingNow
            presence.onSite -> text.waitingFifteen
            else -> text.sleepingGeofence
        }
    }

    private fun statusDetail(
        distanceReading: DistanceReading,
        geofenceMessage: String,
        text: AppText,
        language: AppLanguage,
    ): String {
        return when (distanceReading) {
            DistanceReading.Loading -> text.waitingGps
            is DistanceReading.Ready -> distanceLabel(distanceReading, text, language)
            is DistanceReading.Approximate -> distanceLabel(distanceReading, text, language)
            DistanceReading.NoPermission -> text.allowLocationDistance
            DistanceReading.NoWorkplace -> text.setWorkplace
            DistanceReading.Unavailable -> geofenceMessage
            DistanceReading.Idle -> geofenceMessage
        }
    }

    private fun distanceLabel(distanceReading: DistanceReading, text: AppText, language: AppLanguage): String {
        return when (distanceReading) {
            is DistanceReading.Ready -> if (distanceReading.inside) {
                text.insideZone(DistanceFormatter.formatMeters(distanceReading.meters, language))
            } else {
                text.distanceToWork(DistanceFormatter.formatMeters(distanceReading.meters, language))
            }
            is DistanceReading.Approximate -> text.workplaceRadius(distanceReading.radiusMeters)
            DistanceReading.Loading -> text.waitingGps
            DistanceReading.NoPermission -> text.noLocationPermission
            DistanceReading.NoWorkplace -> text.workplaceMissing
            DistanceReading.Unavailable -> text.distanceUnavailable
            DistanceReading.Idle -> text.radiusPending
        }
    }
}
