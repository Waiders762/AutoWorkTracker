package com.autoworktracker.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.autoworktracker.AutoWorkTrackerApp
import com.autoworktracker.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ShiftActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noBreak = intent.getBooleanExtra(EXTRA_NO_BREAK, false)
        val pending = goAsync()
        val app = context.applicationContext as AutoWorkTrackerApp
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                AppLogger.ui("Notification lunch action noBreak=$noBreak")
                app.container.lunchStatus.setNoAfternoonBreak(noBreak, "notification")
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_NO_BREAK = "no_break"
        const val ACTION_SET = "com.autoworktracker.action.SET_LUNCH"
    }
}
