@file:Suppress("DEPRECATION")

package com.navigine.navigine.demo.application

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.navigine.navigine.demo.utils.DimensionUtils
import com.navigine.sdk.Navigine

class NavigineApp : Application(), LifecycleObserver {
    override fun onCreate() {
        super.onCreate()

        AppContext = getApplicationContext()

        val displayMetrics = getResources().getDisplayMetrics()
        DimensionUtils.setDisplayMetrics(displayMetrics)

        Navigine.initialize(getApplicationContext())

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
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
