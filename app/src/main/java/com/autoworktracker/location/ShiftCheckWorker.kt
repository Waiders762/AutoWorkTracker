package com.autoworktracker.location

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.autoworktracker.AutoWorkTrackerApp

class ShiftCheckWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val ruleId = inputData.getString(KEY_RULE_ID)
        if (ruleId.isNullOrBlank()) return Result.failure()
        val app = applicationContext as? AutoWorkTrackerApp ?: return Result.failure()
        return try {
            app.container.shiftCoordinator.onSafetyCheck(ruleId)
            if (inputData.getString(KEY_MODE) == MODE_FALLBACK) {
                val rules = app.container.settingsRepository.current().rules
                app.container.alarmScheduler.reschedule(rules)
            }
            Result.success()
        } catch (error: Exception) {
            Log.e(TAG, "Shift check failed", error)
            Result.retry()
        }
    }

    companion object {
        const val KEY_RULE_ID = "rule_id"
        const val KEY_MODE = "mode"
        const val MODE_ALARM = "alarm"
        const val MODE_FALLBACK = "fallback"
        private const val TAG = "AutoWork"
    }
}
