package com.autoworktracker.location

import android.content.Context
import com.autoworktracker.domain.model.GeofenceStatus
import com.autoworktracker.domain.repository.SettingsRepository

class MonitoringMaintenance(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val geofenceManager: GeofenceManager,
    private val alarmScheduler: ShiftAlarmScheduler,
) {
    suspend fun ensureMonitoring() {
        rescheduleAlarms()
        ensureGeofence()
    }

    suspend fun rescheduleAlarms() {
        alarmScheduler.reschedule(settingsRepository.current().rules)
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
