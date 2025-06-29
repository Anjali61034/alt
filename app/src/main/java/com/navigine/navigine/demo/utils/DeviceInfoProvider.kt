package com.navigine.navigine.demo.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.Locale

object DeviceInfoProvider {
    fun getDeviceId(context: Context): String {
        return generateDeviceID(context)
    }

    private fun generateDeviceID(context: Context): String {
        try {
            val id = getAndroidID(context)
            if (id.length > 0) {
                return id
            }

            if (Build.SERIAL != null && Build.SERIAL != "UNKNOWN") {
                return Build.SERIAL
            }
        } catch (var1: Throwable) {
            var1.printStackTrace()
        }

        return deviceHash
    }


    private fun getAndroidID(context: Context): String {
        val str = Settings.Secure.getString(context.getContentResolver(), "android_id")
        return if (str == null) "" else str.uppercase(Locale.getDefault())
    }

    private val deviceHash: String
        get() {
            val str =
                Build.BOARD + Build.BRAND + Build.CPU_ABI + Build.DEVICE + Build.DISPLAY + Build.HOST + Build.ID + Build.MANUFACTURER + Build.MODEL + Build.PRODUCT + Build.TAGS + Build.TYPE + Build.USER + Build.FINGERPRINT
            return HashingProvider.md5(str)
        }
}
