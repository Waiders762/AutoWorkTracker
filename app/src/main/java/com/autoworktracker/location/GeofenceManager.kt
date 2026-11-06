package com.autoworktracker.location

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.autoworktracker.domain.model.GeofenceStatus
import com.autoworktracker.domain.model.Workplace
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

data class GeofenceRegistration(
    val active: Boolean = false,
    val status: GeofenceStatus = GeofenceStatus.NotRegistered,
)

class GeofenceManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val callbackExecutor = Executors.newSingleThreadExecutor()
    private val mutableRegistration = MutableStateFlow(GeofenceRegistration())
    val registration: StateFlow<GeofenceRegistration> = mutableRegistration.asStateFlow()

    fun markInactive(status: GeofenceStatus) {
        mutableRegistration.value = GeofenceRegistration(active = false, status = status)
    }

    suspend fun register(workplace: Workplace) {
        if (!LocationPermissions.canMonitor(appContext)) {
            markInactive(GeofenceStatus.NeedBackgroundPermission)
            return
        }
        val playServices = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(appContext)
        if (playServices != ConnectionResult.SUCCESS) {
            markInactive(GeofenceStatus.PlayServicesUnavailable)
            return
        }
        val safe = workplace.sanitized()
        val geofence = Geofence.Builder()
            .setRequestId(REQUEST_ID)
            .setCircularRegion(safe.latitude, safe.longitude, safe.radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or
                    Geofence.GEOFENCE_TRANSITION_EXIT or
                    Geofence.GEOFENCE_TRANSITION_DWELL,
            )
            .setLoiteringDelay(LOITERING_DELAY_MS)
            .setNotificationResponsiveness(NOTIFICATION_RESPONSIVENESS_MS)
            .build()
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(
                GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL,
            )
            .addGeofence(geofence)
            .build()
        try {
            withTimeout(8_000) {
                suspendCancellableCoroutine { continuation ->
                    LocationServices.getGeofencingClient(appContext)
                        .addGeofences(request, geofencePendingIntent())
                        .addOnSuccessListener(callbackExecutor) {
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                        .addOnFailureListener(callbackExecutor) { error ->
                            if (continuation.isActive) continuation.resumeWithException(error)
                        }
                }
            }
            mutableRegistration.value = GeofenceRegistration(
                active = true,
                status = GeofenceStatus.Active(safe.radiusMeters.toInt()),
            )
            Log.i(TAG, "Geofence registered")
        } catch (error: TimeoutCancellationException) {
            mutableRegistration.value = GeofenceRegistration(
                active = false,
                status = GeofenceStatus.Timeout,
            )
            Log.e(TAG, "Geofence registration timed out", error)
        } catch (error: Exception) {
            val code = (error as? ApiException)?.statusCode
            mutableRegistration.value = GeofenceRegistration(
                active = false,
                status = GeofenceStatus.Error(code),
            )
            Log.e(TAG, "Geofence registration failed", error)
        }
    }

    fun geofencePendingIntent(): PendingIntent {
        val intent = Intent(appContext, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            appContext,
            PENDING_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    companion object {
        const val REQUEST_ID = "workplace"
        const val LOITERING_DELAY_MS = 15 * 60 * 1000
        const val NOTIFICATION_RESPONSIVENESS_MS = 120_000
        private const val PENDING_REQUEST_CODE = 1001
        private const val TAG = "AutoWork"
    }
}
