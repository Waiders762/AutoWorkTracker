package com.autoworktracker.location

import android.util.Log
import com.autoworktracker.domain.model.PresenceState
import com.autoworktracker.domain.model.Shift
import com.autoworktracker.domain.model.ShiftSource
import com.autoworktracker.domain.repository.SettingsRepository
import com.autoworktracker.domain.repository.ShiftRepository
import com.autoworktracker.domain.model.AppSettings
import com.autoworktracker.domain.usecase.EveningShiftHours
import com.autoworktracker.domain.usecase.EvaluateExitUseCase
import com.autoworktracker.domain.usecase.ExitDecision
import com.autoworktracker.logging.AppLogger
import com.autoworktracker.domain.model.AppLanguage
import com.autoworktracker.domain.util.AppText
import com.autoworktracker.domain.util.RussianDateFormatter
import com.autoworktracker.notification.NotificationHelper
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ShiftCoordinator(
    private val settingsRepository: SettingsRepository,
    private val shiftRepository: ShiftRepository,
    private val evaluateExit: EvaluateExitUseCase,
    private val notifications: NotificationHelper,
    private val locationHelper: LocationHelper,
) {
    private val mutex = Mutex()

    suspend fun onEnter(epochMillis: Long = System.currentTimeMillis()) = mutex.withLock {
        val zone = ZoneId.systemDefault()
        val today = epochMillis.toLocalDate(zone)
        val settings = settingsRepository.current()
        val text = AppText.of(settings.language)
        val stored = settings.presence
        val presence = if (stored.isForDay(today, zone)) stored else PresenceState()
        if (presence.onSite) return@withLock
        val updated = presence.copy(
            onSite = true,
            firstEntryEpochMillis = presence.firstEntryEpochMillis ?: epochMillis,
            lastEntryEpochMillis = epochMillis,
            dwellConfirmed = false,
        )
        settingsRepository.updatePresence(updated)
        val clock = RussianDateFormatter.formatTime(epochMillis.toLocalTime(zone))
        AppLogger.geofence("ENTER onSite=true")
        notifications.notifyPresence(
            text.onShiftTitle(),
            text.onShiftBody(clock),
            settings.language,
            settings.noBreakHours,
            longHours(settings),
        )
    }

    suspend fun onDwell(epochMillis: Long = System.currentTimeMillis()) = mutex.withLock {
        val zone = ZoneId.systemDefault()
        val today = epochMillis.toLocalDate(zone)
        val settings = settingsRepository.current()
        val text = AppText.of(settings.language)
        val stored = settings.presence
        val presence = if (stored.isForDay(today, zone)) stored else PresenceState()
        val updated = presence.copy(
            onSite = true,
            dwellConfirmed = true,
            sessionConfirmed = true,
            firstEntryEpochMillis = presence.firstEntryEpochMillis ?: epochMillis,
            lastEntryEpochMillis = presence.lastEntryEpochMillis ?: epochMillis,
        )
        settingsRepository.updatePresence(updated)
        if (!presence.dwellConfirmed) {
            AppLogger.geofence("DWELL confirmed")
            notifications.notifyPresence(
                text.dwellTitle(),
                text.dwellBody(),
                settings.language,
                settings.noBreakHours,
                longHours(settings),
            )
        }
    }

    suspend fun onExit(epochMillis: Long = System.currentTimeMillis()) = mutex.withLock {
        handleExitLocked(epochMillis, ShiftSource.AUTO_GEOFENCE)
    }

    suspend fun onSafetyCheck(ruleId: String, epochMillis: Long = System.currentTimeMillis()) = mutex.withLock {
        val settings = settingsRepository.current()
        val workplace = settings.workplace ?: return@withLock
        val rule = settings.rules.firstOrNull { it.id == ruleId && it.enabled && it.alarmBackupEnabled }
            ?: return@withLock
        val zone = ZoneId.systemDefault()
        if (!rule.contains(epochMillis.toLocalTime(zone))) return@withLock
        val presence = effectivePresence(settings.presence, epochMillis, zone)
        if (!presence.onSite) return@withLock
        val location = locationHelper.currentPrecise()
        if (location == null || !location.hasAccuracy() || location.accuracy > 50f) {
            Log.i(TAG, "Safety check skipped: accuracy=${location?.accuracy}")
            return@withLock
        }
        val distance = locationHelper.distanceMeters(
            location.latitude,
            location.longitude,
            workplace.latitude,
            workplace.longitude,
        )
        val outside = distance > workplace.radiusMeters + OUTSIDE_BUFFER_METERS
        if (!outside) return@withLock
        handleExitLocked(epochMillis, ShiftSource.AUTO_CHECK)
    }

    private suspend fun handleExitLocked(epochMillis: Long, source: ShiftSource) {
        val zone = ZoneId.systemDefault()
        val today = epochMillis.toLocalDate(zone)
        val exitTime = epochMillis.toLocalTime(zone).withSecond(0).withNano(0)
        val settings = settingsRepository.current()
        val text = AppText.of(settings.language)
        val presence = effectivePresence(settings.presence, epochMillis, zone)
        val stayedMs = presence.lastEntryEpochMillis?.let { epochMillis - it } ?: 0L
        val realVisit = presence.dwellConfirmed || stayedMs >= LOITER_MS
        if (!realVisit) {
            settingsRepository.updatePresence(presence.copy(onSite = false, dwellConfirmed = false))
            AppLogger.geofence("EXIT short visit, hours not saved")
            notifications.notifyPresence(
                text.shortVisitTitle(),
                text.shortVisitBody(),
                settings.language,
                settings.noBreakHours,
                longHours(settings),
            )
            return
        }
        val existing = shiftRepository.getByDate(today)
        when (
            val decision = evaluateExit.decide(
                exitTime = exitTime,
                rules = settings.rules,
                existingHours = existing?.hours,
                overwriteEnabled = settings.overwriteShortWithLong,
            )
        ) {
            is ExitDecision.Apply -> {
                val hours = EveningShiftHours.resolve(
                    rule = decision.rule,
                    noAfternoonBreak = presence.noAfternoonBreakToday,
                    noBreakHours = settings.noBreakHours,
                )
                AppLogger.geofence(
                    "EXIT apply rule=${decision.rule.id} hours=$hours noBreak=${presence.noAfternoonBreakToday}",
                )
                saveShift(
                    today,
                    exitTime,
                    presence,
                    existing,
                    hours,
                    source,
                    epochMillis,
                    settings.language,
                )
            }
            is ExitDecision.KeepExisting -> AppLogger.geofence("EXIT keep existing hours")
            ExitDecision.OutsideWindow -> {
                AppLogger.geofence("EXIT outside window existing=${existing != null}")
                if (existing == null) {
                    notifications.notifyPresence(
                        text.outsideWindowTitle(),
                        text.outsideWindowBody(RussianDateFormatter.formatTime(exitTime)),
                        settings.language,
                        settings.noBreakHours,
                        longHours(settings),
                    )
                }
            }
        }
        settingsRepository.updatePresence(
            presence.copy(
                onSite = false,
                dwellConfirmed = false,
                sessionConfirmed = true,
            ),
        )
    }

    private suspend fun saveShift(
        today: LocalDate,
        exitTime: LocalTime,
        presence: PresenceState,
        existing: Shift?,
        hours: Double,
        source: ShiftSource,
        epochMillis: Long,
        language: AppLanguage,
    ) {
        val zone = ZoneId.systemDefault()
        val entryTime = presence.firstEntryEpochMillis?.let { entry ->
            if (entry.toLocalDate(zone) == today) entry.toLocalTime(zone).withSecond(0).withNano(0) else existing?.entryTime
        } ?: existing?.entryTime
        val shift = Shift(
            id = existing?.id ?: 0,
            date = today,
            hours = hours,
            entryTime = entryTime,
            exitTime = exitTime,
            source = source,
            note = existing?.note.orEmpty(),
            updatedAtEpochMillis = epochMillis,
        )
        val changed = existing == null || abs(existing.hours - shift.hours) > 0.001
        shiftRepository.upsert(shift)
        if (changed) notifications.notifyShiftRecorded(shift, language)
    }

    private fun longHours(settings: AppSettings): Double {
        return settings.rules.firstOrNull { it.id == "long" }?.hours ?: 11.0
    }

    private fun effectivePresence(presence: PresenceState, epochMillis: Long, zone: ZoneId): PresenceState {
        return if (presence.isForDay(epochMillis.toLocalDate(zone), zone)) presence else PresenceState()
    }

    private fun Long.toLocalDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

    private fun Long.toLocalTime(zone: ZoneId): LocalTime = Instant.ofEpochMilli(this).atZone(zone).toLocalTime()

    private companion object {
        const val LOITER_MS = 15 * 60 * 1000L
        const val OUTSIDE_BUFFER_METERS = 80f
        const val TAG = "AutoWork"
    }
}
