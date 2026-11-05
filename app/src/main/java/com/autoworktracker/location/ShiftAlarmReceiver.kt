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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ShiftAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ruleId = intent.getStringExtra(ShiftCheckWorker.KEY_RULE_ID)
        if (ruleId.isNullOrBlank()) return
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
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
                val app = appContext as AutoWorkTrackerApp
                app.container.shiftCoordinator.onSafetyCheck(ruleId)
                val rules = app.container.settingsRepository.current().rules
                app.container.alarmScheduler.reschedule(rules)
            } catch (error: Exception) {
                Log.e(TAG, "Alarm shift check failed", error)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AutoWork"
    }
}
