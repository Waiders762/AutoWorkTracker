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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LunchBreakDetector(
    context: Context,
    private val settingsRepository: SettingsRepository,
    private val notifications: NotificationHelper,
    private val alarmScheduler: ShiftAlarmScheduler,
) {
    private val appContext = context.applicationContext
    private val store = LunchSessionStore(appContext)
    private val sensorManager = appContext.getSystemService(SensorManager::class.java)
    private val powerManager = appContext.getSystemService(PowerManager::class.java)
    private val keyguardManager = appContext.getSystemService(KeyguardManager::class.java)
    private val mutex = Mutex()
    private val sampling = AtomicBoolean(false)

    suspend fun startWindow() {
        val sampleNow = mutex.withLock { beginSession() }
        if (sampleNow) onSample()
    }

    suspend fun onSample() {
        if (!sampling.compareAndSet(false, true)) return
        try {
            val session = mutex.withLock {
                val current = runningSession() ?: return@withLock null
                if (System.currentTimeMillis() > current.endAtMillis) {
                    finishLocked(current)
                    null
                } else {
                    current
                }
            } ?: return
            val unlockedNow = isUnlocked()
            val motion = measureMotion()
            mutex.withLock {
                val current = runningSession() ?: return@withLock
                if (current.day != session.day || current.endAtMillis != session.endAtMillis) return@withLock
                val activeNow = LunchVerdict.minuteWasActive(motion.average, motion.peak)
                val updated = current.copy(
                    unlockedSeconds = current.unlockedSeconds + if (unlockedNow) SECONDS_PER_SAMPLE else 0,
                    activeMinutes = current.activeMinutes + if (activeNow) 1 else 0,
                    stillMinutes = current.stillMinutes + if (activeNow) 0 else 1,
                    sampledMinutes = current.sampledMinutes + 1,
                )
                AppLogger.sensors(
                    "Minute ${updated.sampledMinutes} unlockedSec=${updated.unlockedSeconds} active=$activeNow peakDeltaG=${motion.peak} avgDeltaG=${motion.average} activeMin=${updated.activeMinutes} stillMin=${updated.stillMinutes}",
                )
                val nextAt = System.currentTimeMillis() + INTERVAL_MS
                if (nextAt > updated.endAtMillis) {
                    finishLocked(updated)
                } else {
                    store.write(updated.copy(nextAtMillis = nextAt))
                    alarmScheduler.scheduleLunchSample(nextAt)
                }
            }
        } finally {
            sampling.set(false)
        }
    }

    suspend fun requestFinish() {
        mutex.withLock {
            val session = runningSession()
            if (session == null) {
                AppLogger.sensors("Lunch window end with no active session")
                alarmScheduler.cancelLunchSample()
                return@withLock
            }
            finishLocked(session)
        }
    }

    private suspend fun beginSession(): Boolean {
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val today = LocalDate.now(zone).toString()
        val existing = store.read()
        if (existing.active && existing.day == today) {
            if (now > existing.endAtMillis) {
                finishLocked(existing)
                return false
            }
            if (existing.nextAtMillis <= now) {
                val soon = now + RECOVERY_DELAY_MS
                store.write(existing.copy(nextAtMillis = soon))
                alarmScheduler.scheduleLunchSample(soon)
                AppLogger.sensors("Lunch sample resumed")
            }
            AppLogger.sensors("Lunch detector already running")
            return false
        }
        if (existing.active) store.clear()
        val settings = settingsRepository.current()
        if (!settings.autoDetectLunchBreak) {
            AppLogger.sensors("Lunch detector skipped: auto detection is off")
            return false
        }
        val time = LocalTime.now(zone)
        if (time.isBefore(settings.lunchWindowStart) || time.isAfter(settings.lunchWindowEnd)) {
            AppLogger.sensors("Lunch detector skipped: $time is outside ${settings.lunchWindowStart}-${settings.lunchWindowEnd}")
            return false
        }
        if (!settings.presence.onSite) {
            AppLogger.sensors("Lunch detector skipped: not on site")
            return false
        }
        val endAt = LocalDate.now(zone)
            .atTime(settings.lunchWindowEnd)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        store.write(
            LunchSession(
                active = true,
                day = today,
                unlockedSeconds = 0,
                activeMinutes = 0,
                stillMinutes = 0,
                sampledMinutes = 0,
                endAtMillis = endAt,
                nextAtMillis = now,
            ),
        )
        AppLogger.sensors("Lunch detector started until ${settings.lunchWindowEnd} onSite=true")
        return true
    }

    private fun runningSession(): LunchSession? {
        val session = store.read()
        val today = LocalDate.now(ZoneId.systemDefault()).toString()
        if (!session.active || session.day != today) return null
        return session
    }

    private suspend fun finishLocked(session: LunchSession) {
        store.clear()
        alarmScheduler.cancelLunchSample()
        val settings = settingsRepository.current()
        val noBreak = LunchVerdict.workedWithoutBreak(
            unlockedSeconds = session.unlockedSeconds,
            activeMinutes = session.activeMinutes,
            maxUnlockedMinutes = settings.lunchMaxUnlockedMinutes,
            minActiveMinutes = settings.lunchMinActiveMinutes,
        )
        val hours = if (noBreak) settings.noBreakHours else settings.rules.firstOrNull { it.id == "long" }?.hours ?: 11.0
        AppLogger.sensors(
            "Lunch verdict noBreak=$noBreak unlockedSec=${session.unlockedSeconds} activeMin=${session.activeMinutes} stillMin=${session.stillMinutes} hours=$hours",
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

    private fun isUnlocked(): Boolean {
        return powerManager.isInteractive && !keyguardManager.isDeviceLocked
    }

    private suspend fun measureMotion(): MotionReading {
        val lock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AutoWorkTracker:lunch-sample").apply {
            setReferenceCounted(false)
            acquire(SAMPLE_WAKE_MS)
        }
        try {
            return readSensor()
        } finally {
            runCatching { if (lock.isHeld) lock.release() }
        }
    }

    private suspend fun readSensor(): MotionReading {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor == null) {
            AppLogger.sensors("Accelerometer missing")
            return MotionReading(0f, 0f)
        }
        return suspendCancellableCoroutine { continuation ->
            val bucket = MotionBucket()
            val thread = HandlerThread("lunch-sample").apply { start() }
            val handler = Handler(thread.looper)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val x = event.values.getOrNull(0) ?: return
                    val y = event.values.getOrNull(1) ?: return
                    val z = event.values.getOrNull(2) ?: return
                    val delta = abs(sqrt(x * x + y * y + z * z) - GRAVITY)
                    bucket.sum += delta
                    bucket.count++
                    if (delta > bucket.peak) bucket.peak = delta
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            val finish = Runnable {
                sensorManager.unregisterListener(listener)
                thread.quitSafely()
                val average = if (bucket.count == 0) 0f else (bucket.sum / bucket.count).toFloat()
                if (continuation.isActive) continuation.resume(MotionReading(average, bucket.peak))
            }
            continuation.invokeOnCancellation {
                sensorManager.unregisterListener(listener)
                handler.removeCallbacks(finish)
                thread.quitSafely()
            }
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL, handler)
            handler.postDelayed(finish, SAMPLE_MS)
        }
    }

    private data class MotionBucket(
        var peak: Float = 0f,
        var sum: Double = 0.0,
        var count: Int = 0,
    )

    private data class MotionReading(val average: Float, val peak: Float)

    private data class LunchSession(
        val active: Boolean,
        val day: String,
        val unlockedSeconds: Int,
        val activeMinutes: Int,
        val stillMinutes: Int,
        val sampledMinutes: Int,
        val endAtMillis: Long,
        val nextAtMillis: Long,
    )

    private class LunchSessionStore(context: Context) {
        private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        fun read(): LunchSession {
            if (!prefs.getBoolean(KEY_ACTIVE, false)) return inactive()
            return LunchSession(
                active = true,
                day = prefs.getString(KEY_DAY, "").orEmpty(),
                unlockedSeconds = prefs.getInt(KEY_UNLOCKED, 0),
                activeMinutes = prefs.getInt(KEY_ACTIVE_MIN, 0),
                stillMinutes = prefs.getInt(KEY_STILL_MIN, 0),
                sampledMinutes = prefs.getInt(KEY_SAMPLES, 0),
                endAtMillis = prefs.getLong(KEY_END, 0L),
                nextAtMillis = prefs.getLong(KEY_NEXT, 0L),
            )
        }

        fun write(session: LunchSession) {
            prefs.edit()
                .putBoolean(KEY_ACTIVE, session.active)
                .putString(KEY_DAY, session.day)
                .putInt(KEY_UNLOCKED, session.unlockedSeconds)
                .putInt(KEY_ACTIVE_MIN, session.activeMinutes)
                .putInt(KEY_STILL_MIN, session.stillMinutes)
                .putInt(KEY_SAMPLES, session.sampledMinutes)
                .putLong(KEY_END, session.endAtMillis)
                .putLong(KEY_NEXT, session.nextAtMillis)
                .commit()
        }

        fun clear() {
            prefs.edit().clear().commit()
        }

        private fun inactive() = LunchSession(
            active = false,
            day = "",
            unlockedSeconds = 0,
            activeMinutes = 0,
            stillMinutes = 0,
            sampledMinutes = 0,
            endAtMillis = 0L,
            nextAtMillis = 0L,
        )

        private companion object {
            const val PREFS = "lunch_session"
            const val KEY_ACTIVE = "active"
            const val KEY_DAY = "day"
            const val KEY_UNLOCKED = "unlocked"
            const val KEY_ACTIVE_MIN = "active_min"
            const val KEY_STILL_MIN = "still_min"
            const val KEY_SAMPLES = "samples"
            const val KEY_END = "end_at"
            const val KEY_NEXT = "next_at"
        }
    }

    private companion object {
        const val INTERVAL_MS = 60_000L
        const val SAMPLE_MS = 4_000L
        const val SAMPLE_WAKE_MS = 15_000L
        const val RECOVERY_DELAY_MS = 3_000L
        const val SECONDS_PER_SAMPLE = 60
        const val GRAVITY = 9.81f
    }
}
