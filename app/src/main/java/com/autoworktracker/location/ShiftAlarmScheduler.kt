package com.autoworktracker.location

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.autoworktracker.domain.model.AppSettings
import com.autoworktracker.domain.model.ShiftRule
import com.autoworktracker.logging.AppLogger
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class ShiftAlarmScheduler(private val context: Context) {
    fun reschedule(settings: AppSettings) {
        rescheduleRules(settings.rules)
        rescheduleLunch(settings)
    }

    private fun rescheduleRules(rules: List<ShiftRule>) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        for (slot in 0 until SLOT_COUNT) {
            alarmManager.cancel(pendingIntent(slot, ""))
        }
        val backedUp = rules.filter { it.enabled && it.alarmBackupEnabled }
        backedUp.forEachIndexed { index, rule ->
            if (index >= SLOT_COUNT) return@forEachIndexed
            val triggerAt = nextTriggerMillis(rule.checkpoint())
            schedule(alarmManager, triggerAt, pendingIntent(index, rule.id))
        }
        val workManager = WorkManager.getInstance(context)
        rules.forEach { rule -> workManager.cancelUniqueWork(fallbackName(rule.id)) }
        if (!canScheduleExact(alarmManager)) {
            backedUp.forEach { rule -> enqueueFallback(workManager, rule) }
        }
        AppLogger.alarm("Rule alarms scheduled count=${backedUp.size} exact=${canScheduleExact(alarmManager)}")
    }

    private fun rescheduleLunch(settings: AppSettings) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(lunchIntent(start = true))
        alarmManager.cancel(lunchIntent(start = false))
        if (!settings.autoDetectLunchBreak) {
            AppLogger.alarm("Lunch alarms cancelled")
            return
        }
        val startAt = nextTriggerMillis(settings.lunchWindowStart)
        val endAt = nextTriggerMillis(settings.lunchWindowEnd)
        schedule(alarmManager, startAt, lunchIntent(start = true))
        schedule(alarmManager, endAt, lunchIntent(start = false))
        AppLogger.alarm(
            "Lunch alarms set start=${settings.lunchWindowStart} end=${settings.lunchWindowEnd} exact=${canScheduleExact(alarmManager)}",
        )
    }

    private fun lunchIntent(start: Boolean): PendingIntent {
        val intent = Intent(context, ShiftAlarmReceiver::class.java).apply {
            action = if (start) ShiftAlarmReceiver.ACTION_LUNCH_START else ShiftAlarmReceiver.ACTION_LUNCH_END
        }
        return PendingIntent.getBroadcast(
            context,
            if (start) LUNCH_START_REQUEST else LUNCH_END_REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun enqueueFallback(workManager: WorkManager, rule: ShiftRule) {
        val delay = (nextTriggerMillis(rule.checkpoint()) - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<ShiftCheckWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    ShiftCheckWorker.KEY_RULE_ID to rule.id,
                    ShiftCheckWorker.KEY_MODE to ShiftCheckWorker.MODE_FALLBACK,
                ),
            )
            .build()
        workManager.enqueueUniqueWork(fallbackName(rule.id), ExistingWorkPolicy.REPLACE, request)
    }

    private fun schedule(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        val exactAllowed = canScheduleExact(alarmManager)
        if (exactAllowed) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun pendingIntent(slot: Int, ruleId: String): PendingIntent {
        val intent = Intent(context, ShiftAlarmReceiver::class.java).apply {
            putExtra(ShiftCheckWorker.KEY_RULE_ID, ruleId)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_BASE + slot,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun canScheduleExact(alarmManager: AlarmManager): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    private fun fallbackName(ruleId: String): String = "shift-fallback-$ruleId"

    companion object {
        const val SLOT_COUNT = 16
        private const val REQUEST_BASE = 2400
        private const val LUNCH_START_REQUEST = 2610
        private const val LUNCH_END_REQUEST = 2611

        fun nextTriggerMillis(time: LocalTime, now: ZonedDateTime = ZonedDateTime.now()): Long {
            var candidate = now.toLocalDate().atTime(time).atZone(now.zone)
            if (!candidate.toInstant().isAfter(now.toInstant())) {
                candidate = candidate.plusDays(1)
            }
            return candidate.toInstant().toEpochMilli()
        }
    }
}
