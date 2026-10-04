package com.alvarosega.trackingventas.data.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

data class MotionSnapshot(
    val isMoving: Boolean,
    val stepCount: Int,
    val motionVariance: Float
)

@Singleton
class MotionHardwareManager @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val lock = Any()
    private var sampleCount: Long = 0
    private var mean: Double = 0.0
    private var m2: Double = 0.0

    companion object {
        // En reposo sobre mesa: varianza < 0.15
        // En bolsillo caminando o en transporte público (vibración/frenadas): varianza >= 0.60
        private const val MOTION_VARIANCE_THRESHOLD = 0.60f
    }

    fun startListening() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
        synchronized(lock) {
            sampleCount = 0
            mean = 0.0
            m2 = 0.0
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt((x * x + y * y + z * z).toDouble())

        // Algoritmo de Welford: cálculo continuo de varianza en O(1)
        synchronized(lock) {
            sampleCount++
            val delta = magnitude - mean
            mean += delta / sampleCount
            val delta2 = magnitude - mean
            m2 += delta * delta2
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * Extrae la varianza acumulada de todas las muestras del intervalo de 60 segundos
     * y resetea el acumulador para el siguiente ciclo.
     */
    fun consumeMotionSnapshot(): MotionSnapshot {
        val variance: Float

        synchronized(lock) {
            variance = if (sampleCount > 1) {
                (m2 / (sampleCount - 1)).toFloat()
            } else {
                0.0f
            }
            sampleCount = 0
            mean = 0.0
            m2 = 0.0
        }

        val isMoving = variance >= MOTION_VARIANCE_THRESHOLD

        return MotionSnapshot(
            isMoving = isMoving,
            stepCount = 0, // Fijo en 0: no solicita el permiso intrusivo ACTIVITY_RECOGNITION
            motionVariance = variance
        )
    }
}