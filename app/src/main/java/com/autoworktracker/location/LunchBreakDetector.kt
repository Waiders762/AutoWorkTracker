package com.autoworktracker.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.app.KeyguardManager
import com.autoworktracker.domain.repository.SettingsRepository
import com.autoworktracker.domain.usecase.LunchVerdict
import com.autoworktracker.domain.util.AppText
import com.autoworktracker.domain.util.HoursFormatter
import com.autoworktracker.logging.AppLogger
import com.autoworktracker.notification.NotificationHelper
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LunchBreakDetector(
    context: Context,
    private val settingsRepository: SettingsRepository,
    private val notifications: NotificationHelper,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sensorManager = appContext.getSystemService(SensorManager::class.java)
    private val powerManager = appContext.getSystemService(PowerManager::class.java)
    private val keyguardManager = appContext.getSystemService(KeyguardManager::class.java)
    private val sensorThread = HandlerThread("lunch-sensors").apply { start() }
    private val sensorHandler = Handler(sensorThread.looper)
    private val sampleLock = Any()
    private var peak = 0f
    private var sum = 0.0
    private var samples = 0
    private var unlockedSeconds = 0
    private var activeMinutes = 0
    private var stillMinutes = 0
    private var minuteIndex = 0
    private var sessionOpen = false
    private var job: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val evaluated = AtomicBoolean(false)
    private var listener: SensorEventListener? = null

    suspend fun startWindow() {
        val settings = settingsRepository.current()
        if (!settings.autoDetectLunchBreak) {
            AppLogger.sensors("Lunch detector skipped: auto detection is off")
            return
        }
        val now = LocalTime.now()
        if (now.isBefore(settings.lunchWindowStart) || now.isAfter(settings.lunchWindowEnd)) {
            AppLogger.sensors("Lunch detector skipped: $now is outside ${settings.lunchWindowStart}-${settings.lunchWindowEnd}")
            return
        }
        if (!settings.presence.onSite) {
            AppLogger.sensors("Lunch detector skipped: not on site")
            return
        }
        if (sessionOpen) {
            AppLogger.sensors("Lunch detector already running")
            return
        }
        sessionOpen = true
        evaluated.set(false)
        unlockedSeconds = 0
        activeMinutes = 0
        stillMinutes = 0
        minuteIndex = 0
        resetSamples()
        acquireLock()
        registerSensor()
        val end = settings.lunchWindowEnd
        AppLogger.sensors("Lunch detector started until $end onSite=true")
        job = scope.launch {
            try {
                var ticks = 0
                while (sessionOpen && !LocalTime.now().isAfter(end)) {
                    delay(TICK_MS)
                    if (!sessionOpen) break
                    if (isUnlocked()) unlockedSeconds += 10
                    ticks++
                    if (ticks % 6 == 0) flushMinute()
                }
            } finally {
                finishInternal()
            }
        }
    }

    fun requestFinish() {
        if (!sessionOpen) {
            AppLogger.sensors("Lunch window end with no active session")
            return
        }
        sessionOpen = false
        job?.cancel()
    }

    private fun isUnlocked(): Boolean {
        return powerManager.isInteractive && !keyguardManager.isDeviceLocked
    }

    private fun registerSensor() {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor == null) {
            AppLogger.sensors("Accelerometer missing")
            return
        }
        val created = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values.getOrNull(0) ?: return
                val y = event.values.getOrNull(1) ?: return
                val z = event.values.getOrNull(2) ?: return
                val delta = abs(sqrt(x * x + y * y + z * z) - GRAVITY)
                synchronized(sampleLock) {
                    sum += delta
                    samples++
                    if (delta > peak) peak = delta
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        listener = created
        sensorManager.registerListener(created, sensor, SensorManager.SENSOR_DELAY_NORMAL, sensorHandler)
    }

    private fun flushMinute() {
        val average: Float
        val peakNow: Float
        synchronized(sampleLock) {
            average = if (samples == 0) 0f else (sum / samples).toFloat()
            peakNow = peak
            resetSamples()
        }
        minuteIndex++
        val active = LunchVerdict.minuteWasActive(average, peakNow)
        if (active) activeMinutes++ else stillMinutes++
        AppLogger.sensors(
            "Minute $minuteIndex unlockedSec=$unlockedSeconds active=$active peakDeltaG=$peakNow avgDeltaG=$average activeMin=$activeMinutes stillMin=$stillMinutes",
        )
    }

    private fun resetSamples() {
        peak = 0f
        sum = 0.0
        samples = 0
    }

    private suspend fun finishInternal() {
        if (!evaluated.compareAndSet(false, true)) return
        sessionOpen = false
        flushMinute()
        listener?.let { sensorManager.unregisterListener(it) }
        listener = null
        releaseLock()
        val settings = settingsRepository.current()
        val noBreak = LunchVerdict.workedWithoutBreak(
            unlockedSeconds = unlockedSeconds,
            activeMinutes = activeMinutes,
            maxUnlockedMinutes = settings.lunchMaxUnlockedMinutes,
            minActiveMinutes = settings.lunchMinActiveMinutes,
        )
        val hours = if (noBreak) settings.noBreakHours else settings.rules.firstOrNull { it.id == "long" }?.hours ?: 11.0
        AppLogger.sensors(
            "Lunch verdict noBreak=$noBreak unlockedSec=$unlockedSeconds activeMin=$activeMinutes stillMin=$stillMinutes hours=$hours",
        )
        settingsRepository.updatePresence(
            settings.presence.copy(
                noAfternoonBreakToday = noBreak,
                breakAutoDetected = true,
            ),
        )
        val text = AppText.of(settings.language)
        val formatted = HoursFormatter.formatCompact(hours, settings.language)
        if (noBreak) {
            notifications.notifyPresence(
                text.lunchNoBreakTitle(),
                text.lunchNoBreakBody(formatted),
                settings.language,
                settings.noBreakHours,
                settings.rules.firstOrNull { it.id == "long" }?.hours ?: 11.0,
            )
        } else {
            notifications.notifyPresence(
                text.lunchHadBreakTitle(),
                text.lunchHadBreakBody(formatted),
                settings.language,
                settings.noBreakHours,
                settings.rules.firstOrNull { it.id == "long" }?.hours ?: 11.0,
            )
        }
    }

    private fun acquireLock() {
        if (wakeLock?.isHeld == true) return
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AutoWorkTracker:lunch").apply {
            setReferenceCounted(false)
            acquire(40 * 60 * 1000L)
        }
    }

    private fun releaseLock() {
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
    }

    private companion object {
        const val TICK_MS = 10_000L
        const val GRAVITY = 9.81f
    }
}
