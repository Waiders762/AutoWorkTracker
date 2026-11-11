package com.autoworktracker.location

import com.autoworktracker.domain.repository.SettingsRepository
import com.autoworktracker.domain.repository.ShiftRepository
import com.autoworktracker.domain.util.AppText
import com.autoworktracker.domain.util.HoursFormatter
import com.autoworktracker.logging.AppLogger
import com.autoworktracker.notification.NotificationHelper
import java.time.LocalDate
import java.time.ZoneId

class LunchStatusController(
    private val settingsRepository: SettingsRepository,
    private val shiftRepository: ShiftRepository,
    private val notifications: NotificationHelper,
) {
    suspend fun setNoAfternoonBreak(noBreak: Boolean, reason: String) {
        val settings = settingsRepository.current()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val stored = settings.presence
        val anchored = if (stored.isForDay(today, zone)) {
            stored
        } else {
            stored.copy(firstEntryEpochMillis = System.currentTimeMillis())
        }
        val updated = anchored.copy(
            noAfternoonBreakToday = noBreak,
            breakAutoDetected = false,
        )
        settingsRepository.updatePresence(updated)
        val longHours = settings.rules.firstOrNull { it.id == "long" }?.hours ?: 11.0
        val target = if (noBreak) settings.noBreakHours else longHours
        val existing = shiftRepository.getByDate(today)
        if (existing != null && existing.hours >= 10.0) {
            shiftRepository.upsert(
                existing.copy(
                    hours = target,
                    updatedAtEpochMillis = System.currentTimeMillis(),
                ),
            )
            AppLogger.backup("Lunch adjustment rewrote ${existing.date} ${existing.hours} -> $target")
        }
        AppLogger.ui("Lunch status noBreak=$noBreak via $reason oldShift=${existing?.hours} newHours=$target")
        val text = AppText.of(settings.language)
        val formatted = HoursFormatter.formatCompact(target, settings.language)
        notifications.notifyPresence(
            if (noBreak) text.lunchNoBreakTitle() else text.lunchHadBreakTitle(),
            if (noBreak) text.lunchNoBreakBody(formatted) else text.lunchHadBreakBody(formatted),
            settings.language,
            settings.noBreakHours,
            longHours,
        )
    }
}
