package com.autoworktracker.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.autoworktracker.AutoWorkTrackerApp
import com.autoworktracker.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ShiftAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = appContext as AutoWorkTrackerApp
                when (intent.action) {
                    ACTION_LUNCH_START -> {
                        AppLogger.alarm("Lunch start alarm fired")
                        app.container.lunchDetector.startWindow()
                        app.container.alarmScheduler.reschedule(app.container.settingsRepository.current())
                        return@launch
                    }
                    ACTION_LUNCH_END -> {
                        AppLogger.alarm("Lunch end alarm fired")
                        app.container.lunchDetector.requestFinish()
                        app.container.alarmScheduler.reschedule(app.container.settingsRepository.current())
                        return@launch
                    }
                    ACTION_LUNCH_SAMPLE -> {
                        AppLogger.alarm("Lunch sample alarm fired")
                        app.container.lunchDetector.onSample()
                        return@launch
                    }
                }
                val ruleId = intent.getStringExtra(ShiftCheckWorker.KEY_RULE_ID)
                if (ruleId.isNullOrBlank()) return@launch
                AppLogger.alarm("Rule alarm fired id=$ruleId")
                val request = OneTimeWorkRequestBuilder<ShiftCheckWorker>()
                    .setInputData(
                        workDataOf(
                            ShiftCheckWorker.KEY_RULE_ID to ruleId,
                            ShiftCheckWorker.KEY_MODE to ShiftCheckWorker.MODE_ALARM,
                        ),
                    )
                    .build()
                WorkManager.getInstance(appContext).enqueueUniqueWork(
                    "shift-alarm-$ruleId",
                    ExistingWorkPolicy.REPLACE,
                    request,
                )
                app.container.shiftCoordinator.onSafetyCheck(ruleId)
                app.container.alarmScheduler.reschedule(app.container.settingsRepository.current())
            } catch (error: Exception) {
                Log.e(TAG, "Alarm shift check failed", error)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_LUNCH_START = "com.autoworktracker.action.LUNCH_START"
        const val ACTION_LUNCH_END = "com.autoworktracker.action.LUNCH_END"
        const val ACTION_LUNCH_SAMPLE = "com.autoworktracker.action.LUNCH_SAMPLE"
        private const val TAG = "AutoWork"
    }
}
