package dev.tiltdrive.remote

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import kotlin.math.asin
import kotlin.math.sqrt

/**
 * Wheel angle in degrees for a phone held like a steering wheel (screen facing you, landscape).
 * 0 = upright, clockwise = positive. Uses the gyro-fused rotation vector, so hand shake and
 * vibration do not leak in, and a phone tilted back does not make the angle explode.
 */
class TiltSensor(ctx: Context, private val rotation: () -> Int) : SensorEventListener {
    private val sm = ctx.getSystemService(SensorManager::class.java)
    private val r = FloatArray(9)
    @Volatile var angleDeg = 0f
        private set

    fun start() {
        val s = sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sm.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (s != null) sm.registerListener(this, s, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() = sm.unregisterListener(this)

    override fun onSensorChanged(e: SensorEvent) {
        // "Up" as a unit vector in device coordinates.
        val ux: Float
        val uy: Float
        if (e.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(r, e.values)
            ux = r[6]
            uy = r[7]
        } else {
            val n = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2])
            if (n < 1f) return
            ux = e.values[0] / n
            uy = e.values[1] / n
        }
        // Screen-horizontal component of "up" for the current display rotation.
        val sx = when (rotation()) {
            Surface.ROTATION_90 -> -uy   // screen right = -y of the device
            Surface.ROTATION_180 -> -ux
            Surface.ROTATION_270 -> uy   // screen right = +y of the device
            else -> ux
        }
        angleDeg = Math.toDegrees(asin((-sx).coerceIn(-1f, 1f).toDouble())).toFloat()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
