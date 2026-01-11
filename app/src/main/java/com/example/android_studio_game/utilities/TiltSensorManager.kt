package com.example.android_studio_game.utilities

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.android_studio_game.interfaces.TiltCallback
import kotlin.math.abs

class TiltSensorManager(
    context: Context,
    private val tiltCallback: TiltCallback
) {
    private var enabled = true


    private val sensorManager = context.getSystemService(
        Context.SENSOR_SERVICE
    ) as SensorManager

    private val sensor: Sensor? = sensorManager.getDefaultSensor(
        Sensor.TYPE_ACCELEROMETER
    )

    private var timestamp: Long = 0L

    private lateinit var sensorEventListener: SensorEventListener

    init {
        initEventListener()
    }

    private fun initEventListener() {
        sensorEventListener = object : SensorEventListener {

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

            }

            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                calculateTilt(x, y)
            }
        }
    }

    private fun calculateTilt(x: Float, y: Float) {
        if (System.currentTimeMillis() - timestamp < 150) return
        timestamp = System.currentTimeMillis()

        if (abs(x) >= 3.0f) {
            tiltCallback.tiltX(x)
        }

        tiltCallback.tiltY(y)
    }


    fun start() {
        sensor?.let {
            sensorManager.registerListener(
                sensorEventListener,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    fun stop() {
        sensorManager.unregisterListener(
            sensorEventListener
        )
    }

    fun setEnabled(enable: Boolean) {
        enabled = enable
        if (enabled) start() else stop()
    }

    fun isEnabled(): Boolean {
        return enabled
    }

}