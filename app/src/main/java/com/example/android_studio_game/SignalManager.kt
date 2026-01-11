package com.example.android_studio_game

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast

object SignalManager {

    private lateinit var context: Context

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    private var currentToast: Toast? = null

    fun showToast(text: String) {
        currentToast?.cancel()
        currentToast = Toast.makeText(
            context,
            text,
            Toast.LENGTH_SHORT
        )
        currentToast?.show()
    }


    fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(120)
        }
    }
}
