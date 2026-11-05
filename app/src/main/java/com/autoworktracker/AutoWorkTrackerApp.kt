package com.autoworktracker

import android.app.Application
import android.util.Log
import com.autoworktracker.di.AppContainer
import com.autoworktracker.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AutoWorkTrackerApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannels(this)
        Log.i(TAG, "Application started, scheduling startup off the main thread")
        appScope.launch {
            runCatching { container.appStartup.prepare() }
                .onSuccess { Log.i(TAG, "Startup finished") }
                .onFailure { error -> Log.e(TAG, "Startup failed", error) }
        }
    }

    private companion object {
        const val TAG = "AutoWork"
    }
}
