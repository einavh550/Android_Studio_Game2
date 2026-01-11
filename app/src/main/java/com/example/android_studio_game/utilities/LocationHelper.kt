package com.example.android_studio_game.utilities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

object LocationHelper {

    private lateinit var appContext: Context
    private lateinit var fused: FusedLocationProviderClient

    fun init(context: Context) {
        appContext = context.applicationContext
        fused = LocationServices.getFusedLocationProviderClient(appContext)
    }

    private fun ensureInit() {
        check(::fused.isInitialized) {
            "LocationHelper.init(context) must be called before using LocationHelper"
        }
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun getLastLocation(onResult: (Double?, Double?) -> Unit) {
        ensureInit()

        if (!hasLocationPermission()) {
            onResult(null, null)
            return
        }

        // Safe to call - permission checked above
        fused.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    onResult(loc.latitude, loc.longitude)
                } else {
                    requestSingleUpdate(onResult)
                }
            }
            .addOnFailureListener {
                onResult(null, null)
            }
    }


    @SuppressLint("MissingPermission")
    private fun requestSingleUpdate(onResult: (Double?, Double?) -> Unit) {
        // Permission already verified in getLastLocation()
        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            0L
        ).setMaxUpdates(1).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                fused.removeLocationUpdates(this)
                val loc = result.lastLocation
                if (loc != null) onResult(loc.latitude, loc.longitude)
                else onResult(null, null)
            }
        }

        fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }
}