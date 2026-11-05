package com.autoworktracker.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.autoworktracker.location.LocationPermissions

data class PermissionSnapshot(
    val foregroundLocation: Boolean = false,
    val backgroundLocation: Boolean = false,
    val notifications: Boolean = false,
    val batteryUnrestricted: Boolean = false,
    val exactAlarm: Boolean = false,
    val allFiles: Boolean = false,
) {
    companion object {
        fun capture(context: Context): PermissionSnapshot {
            val notifications = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                true
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            }
            val power = context.getSystemService(PowerManager::class.java)
            val alarm = context.getSystemService(AlarmManager::class.java)
            val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()
            val files = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()
            return PermissionSnapshot(
                foregroundLocation = LocationPermissions.hasForeground(context),
                backgroundLocation = LocationPermissions.hasBackground(context),
                notifications = notifications,
                batteryUnrestricted = power.isIgnoringBatteryOptimizations(context.packageName),
                exactAlarm = exact,
                allFiles = files,
            )
        }
    }
}
