package com.autoworktracker.ui.settings

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.autoworktracker.domain.usecase.LunchVerdict
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MotionProbe(context: Context) {
    private val sensorManager = context.applicationContext.getSystemService(SensorManager::class.java)
    private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val xFlow = MutableStateFlow(0f)
    private val yFlow = MutableStateFlow(0f)
    private val zFlow = MutableStateFlow(0f)
    private val deltaFlow = MutableStateFlow(0f)
    private val movingFlow = MutableStateFlow(false)
    val x: StateFlow<Float> = xFlow.asStateFlow()
    val y: StateFlow<Float> = yFlow.asStateFlow()
    val z: StateFlow<Float> = zFlow.asStateFlow()
    val delta: StateFlow<Float> = deltaFlow.asStateFlow()
    val moving: StateFlow<Boolean> = movingFlow.asStateFlow()
    val available: Boolean = sensor != null
    private var listener: SensorEventListener? = null

    fun start(): Boolean {
        val hardware = sensor ?: return false
        if (listener != null) return true
        val created = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val sx = event.values.getOrElse(0) { 0f }
                val sy = event.values.getOrElse(1) { 0f }
                val sz = event.values.getOrElse(2) { 0f }
                val value = abs(sqrt(sx * sx + sy * sy + sz * sz) - 9.81f)
                xFlow.value = sx
                yFlow.value = sy
                zFlow.value = sz
                deltaFlow.value = value
                movingFlow.value = value > LunchVerdict.MOVEMENT_DELTA_G
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        listener = created
        sensorManager.registerListener(created, hardware, SensorManager.SENSOR_DELAY_NORMAL)
        return true
    }

    fun stop() {
        listener?.let { sensorManager.unregisterListener(it) }
        listener = null
    }
}
