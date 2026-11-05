package com.autoworktracker.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserManager
import android.util.Log
import com.autoworktracker.AutoWorkTrackerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * После перезагрузки, обновления приложения и смены часов заново регистрирует геозону
 * и два точечных будильника на концы смен.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED) return
        val userManager = context.getSystemService(UserManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !userManager.isUserUnlocked) return
        Log.i(TAG, "Restoring monitoring after $action")
        val pending = goAsync()
        val app = context.applicationContext as AutoWorkTrackerApp
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                app.container.maintenance.ensureMonitoring()
                Log.i(TAG, "Monitoring restored after $action")
            } catch (error: Exception) {
                Log.e(TAG, "Failed to restore monitoring after $action", error)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AutoWork"
        val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
