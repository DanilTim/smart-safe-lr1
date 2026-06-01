package com.example.laba1

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager


enum class TiltDirection { LEFT, RIGHT, UP, DOWN }


class TiltDetector(
    context: Context,
    private val cooldownMs: Long = COOLDOWN_MS_DEFAULT,
    private val onTilt: (TiltDirection) -> Unit
) : SensorEventListener {

    companion object {
        const val COOLDOWN_MS_DEFAULT = 600L
    }

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastTiltTime = 0L

    // ── Жизненный цикл ────────────────────────────────────────────────────────


    fun register() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }


    fun unregister() {
        sensorManager.unregisterListener(this)
    }


    fun isAvailable(): Boolean = accelerometer != null

    // ── SensorEventListener ───────────────────────────────────────────────────

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]

        val now = System.currentTimeMillis()
        if (now - lastTiltTime < cooldownMs) return

        val direction: TiltDirection? = when {
            x > 7.0f  -> TiltDirection.LEFT
            x < -7.0f -> TiltDirection.RIGHT
            y > 7.5f  -> TiltDirection.DOWN
            y < -4.5f -> TiltDirection.UP
            else           -> null
        }

        if (direction != null) {
            lastTiltTime = now
            onTilt(direction)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
}