package com.autoworktracker.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.autoworktracker.AutoWorkTrackerApp
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent)
        if (event == null) {
            Log.w(TAG, "Geofence intent without event")
            return
        }
        if (event.hasError()) {
            Log.e(TAG, "Geofence error code ${event.errorCode}")
            return
        }
        val pending = goAsync()
        val app = context.applicationContext as AutoWorkTrackerApp
        val transition = event.geofenceTransition
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (transition) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> app.container.shiftCoordinator.onEnter()
                    Geofence.GEOFENCE_TRANSITION_DWELL -> app.container.shiftCoordinator.onDwell()
                    Geofence.GEOFENCE_TRANSITION_EXIT -> app.container.shiftCoordinator.onExit()
                    else -> Log.w(TAG, "Unknown geofence transition $transition")
                }
            } catch (error: Exception) {
                Log.e(TAG, "Geofence handling failed", error)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AutoWork"
    }
}
