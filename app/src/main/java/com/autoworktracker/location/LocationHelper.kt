package com.autoworktracker.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.autoworktracker.logging.AppLogger
import kotlin.coroutines.resume
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

class LocationHelper(context: Context) {
    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)

    @SuppressLint("MissingPermission")
    suspend fun currentBalanced(timeoutMs: Long = 10_000L): Location? {
        return requestFix(
            priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            granularity = Granularity.GRANULARITY_COARSE,
            timeoutMs = timeoutMs,
            maxUpdateAgeMillis = 120_000L,
        )
    }

    /**
     * One fresh GPS fix while the user is looking at the map.
     * The cached balanced fix is ignored: it is what dropped the pin in the wrong place.
     */
    @SuppressLint("MissingPermission")
    suspend fun currentPrecise(timeoutMs: Long = 12_000L): Location? {
        return requestFix(
            priority = Priority.PRIORITY_HIGH_ACCURACY,
            granularity = Granularity.GRANULARITY_FINE,
            timeoutMs = timeoutMs,
            maxUpdateAgeMillis = 0L,
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFix(
        priority: Int,
        granularity: Int,
        timeoutMs: Long,
        maxUpdateAgeMillis: Long,
    ): Location? {
        if (!LocationPermissions.hasForeground(appContext)) {
            AppLogger.gps("Fix skipped: no foreground permission priority=$priority")
            return null
        }
        AppLogger.gps("Start fix priority=$priority timeoutMs=$timeoutMs")
        val request = CurrentLocationRequest.Builder()
            .setPriority(priority)
            .setGranularity(granularity)
            .setDurationMillis(timeoutMs)
            .setMaxUpdateAgeMillis(maxUpdateAgeMillis)
            .build()
        val cancellation = CancellationTokenSource()
        val result: Location? = try {
            withTimeout(timeoutMs + 1_000L) {
                suspendCancellableCoroutine { continuation ->
                    continuation.invokeOnCancellation { cancellation.cancel() }
                    client.getCurrentLocation(request, cancellation.token)
                        .addOnSuccessListener { location ->
                            if (continuation.isActive) continuation.resume(location)
                        }
                        .addOnFailureListener {
                            if (continuation.isActive) continuation.resume(null)
                        }
                }
            }
        } catch (_: TimeoutCancellationException) {
            cancellation.cancel()
            null
        } catch (_: SecurityException) {
            null
        }
        if (result == null) {
            AppLogger.gps("Fix timeout or empty priority=$priority")
        } else {
            AppLogger.gps("Fix lat=${result.latitude} lng=${result.longitude} accuracy=${result.accuracy}")
        }
        return result
    }

    /**
     * Slow map dot. The camera is centered by one precise fix; this stream only follows the user.
     */
    @SuppressLint("MissingPermission")
    fun fixes(): Flow<Location> = callbackFlow {
        if (!LocationPermissions.hasForeground(appContext)) {
            close()
            return@callbackFlow
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 8_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    fun distanceMeters(latitude: Double, longitude: Double, otherLatitude: Double, otherLongitude: Double): Float {
        val result = FloatArray(1)
        Location.distanceBetween(latitude, longitude, otherLatitude, otherLongitude, result)
        return result[0]
    }
}
