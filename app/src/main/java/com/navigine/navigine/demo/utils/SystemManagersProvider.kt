package com.navigine.navigine.demo.utils

import android.content.Context
import android.net.ConnectivityManager

object SystemManagersProvider {
    private var mConnectivityManager: ConnectivityManager? = null

    @JvmStatic
    fun getConnectivityManager(context: Context): ConnectivityManager? {
        if (mConnectivityManager == null) {
            mConnectivityManager =
                context.getSystemService<ConnectivityManager?>(ConnectivityManager::class.java)
        }
        return mConnectivityManager
    }
}
