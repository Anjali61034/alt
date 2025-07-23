@file:Suppress("DEPRECATION")

package com.navigine.navigine.demo.application

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.navigine.navigine.demo.utils.BeaconScannerManager
import com.navigine.navigine.demo.utils.DimensionUtils
import com.navigine.sdk.Navigine

class NavigineApp : Application(), LifecycleObserver {

    override fun onCreate() {
        super.onCreate()

        // Check permissions and start beacon scanning
        if (hasRequiredPermissions()) {
            BeaconScannerManager.getInstance(this).startScanning()
        }
        AppContext = applicationContext

        val displayMetrics = resources.displayMetrics
        DimensionUtils.setDisplayMetrics(displayMetrics)

        Navigine.initialize(applicationContext)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun hasRequiredPermissions(): Boolean {
        val hasLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasBluetooth = ContextCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
            return hasLocation && hasBluetooth
        }

        return hasLocation
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun onEnterForeground() {
        try {
            Navigine.setMode(Navigine.Mode.NORMAL)
        } catch (e: Throwable) {
            Log.e("NavigineSDK", "Navigine SDK is not initialized yet")
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onResume() {
        try {
            Navigine.setMode(Navigine.Mode.NORMAL)
        } catch (e: Throwable) {
            Log.e("NavigineSDK", "Navigine SDK is not initialized yet")
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun onPause() {
        try {
            Navigine.setMode(Navigine.Mode.BACKGROUND)
        } catch (e: Throwable) {
            Log.e("NavigineSDK", "Navigine SDK is not initialized yet")
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onEnterBackground() {
        try {
            Navigine.setMode(Navigine.Mode.BACKGROUND)
        } catch (e: Throwable) {
            Log.e("NavigineSDK", "Navigine SDK is not initialized yet")
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
        try {
            Navigine.setMode(Navigine.Mode.BACKGROUND)
        } catch (e: Throwable) {
            Log.e("NavigineSDK", "Navigine SDK is not initialized yet")
        }
    }

    companion object {
        @JvmField
        var AppContext: Context? = null
    }
}