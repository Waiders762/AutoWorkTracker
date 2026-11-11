package com.autoworktracker.di

import android.content.Context
import com.autoworktracker.data.backup.BackupManager
import com.autoworktracker.data.backup.BackupRestorer
import com.autoworktracker.data.local.AppDatabase
import com.autoworktracker.data.preferences.SettingsDataStore
import com.autoworktracker.data.repository.SettingsRepositoryImpl
import com.autoworktracker.data.repository.ShiftRepositoryImpl
import com.autoworktracker.domain.repository.SettingsRepository
import com.autoworktracker.domain.repository.ShiftRepository
import com.autoworktracker.domain.usecase.BuildMonthSummaryUseCase
import com.autoworktracker.domain.usecase.EvaluateExitUseCase
import com.autoworktracker.location.GeofenceManager
import com.autoworktracker.location.LunchBreakDetector
import com.autoworktracker.location.LunchStatusController
import com.autoworktracker.location.LocationHelper
import com.autoworktracker.location.MonitoringMaintenance
import com.autoworktracker.location.ShiftAlarmScheduler
import com.autoworktracker.location.ShiftCoordinator
import com.autoworktracker.notification.NotificationHelper
import com.autoworktracker.startup.AppStartup
import com.autoworktracker.startup.StartupState

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database: AppDatabase = AppDatabase.create(appContext)
    private val settingsDataStore = SettingsDataStore(appContext)
    val backupManager = BackupManager(appContext, database.shiftDao(), settingsDataStore)
    val shiftRepository: ShiftRepository = ShiftRepositoryImpl(database, database.shiftDao(), backupManager)
    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(settingsDataStore, backupManager)
    val locationHelper = LocationHelper(appContext)
    val notifications = NotificationHelper(appContext)
    val evaluateExit = EvaluateExitUseCase()
    val monthSummary = BuildMonthSummaryUseCase()
    val shiftCoordinator = ShiftCoordinator(
        settingsRepository = settingsRepository,
        shiftRepository = shiftRepository,
        evaluateExit = evaluateExit,
        notifications = notifications,
        locationHelper = locationHelper,
    )
    val geofenceManager = GeofenceManager(appContext)
    val alarmScheduler = ShiftAlarmScheduler(appContext)
    val lunchDetector = LunchBreakDetector(appContext, settingsRepository, notifications, alarmScheduler)
    val lunchStatus = LunchStatusController(settingsRepository, shiftRepository, notifications)
    val maintenance = MonitoringMaintenance(
        context = appContext,
        settingsRepository = settingsRepository,
        geofenceManager = geofenceManager,
        alarmScheduler = alarmScheduler,
        lunchDetector = lunchDetector,
    )
    val startupState = StartupState()
    val backupRestorer = BackupRestorer(
        context = appContext,
        backupManager = backupManager,
        shiftRepository = shiftRepository,
        settingsRepository = settingsRepository,
        maintenance = maintenance,
        startupState = startupState,
    )
    val appStartup = AppStartup(
        shiftRepository = shiftRepository,
        settingsRepository = settingsRepository,
        backupManager = backupManager,
        maintenance = maintenance,
        startupState = startupState,
    )
}
