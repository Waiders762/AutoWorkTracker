package com.autoworktracker.location

import android.content.Context
import com.autoworktracker.domain.model.GeofenceStatus
import com.autoworktracker.domain.repository.SettingsRepository

class MonitoringMaintenance(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val geofenceManager: GeofenceManager,
    private val alarmScheduler: ShiftAlarmScheduler,
    private val lunchDetector: LunchBreakDetector,
) {
    suspend fun ensureMonitoring() {
        rescheduleAlarms()
        ensureGeofence()
    }

    suspend fun rescheduleAlarms() {
        val settings = settingsRepository.current()
        alarmScheduler.reschedule(settings)
        lunchDetector.startWindow()
    }

    suspend fun ensureGeofence() {
        val workplace = settingsRepository.current().workplace
        if (workplace == null) {
            geofenceManager.markInactive(GeofenceStatus.WorkplaceNotSet)
            return
        }
        if (!LocationPermissions.canMonitor(context)) {
            geofenceManager.markInactive(GeofenceStatus.NeedBackgroundPermission)
            return
        }
        geofenceManager.register(workplace)
    }
}
