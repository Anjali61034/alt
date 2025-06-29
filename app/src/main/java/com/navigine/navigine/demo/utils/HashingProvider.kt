package com.navigine.navigine.demo.utils

import android.util.Log
import java.security.MessageDigest
import java.util.Locale
import com.navigine.navigine.demo.utils.Constants.TAG

object HashingProvider {
    fun md5(s: String): String {
        try {
            val digest = MessageDigest.getInstance("MD5")
            digest.update(s.toByteArray())
            val messageDigest = digest.digest()
            val hexString = StringBuffer()

            for (i in messageDigest.indices) {
                var h = Integer.toHexString(255 and messageDigest[i].toInt())
                while (h.length < 2) {
                    h = "0" + h
                }
                hexString.append(h)
            }

            return hexString.toString().uppercase(Locale.getDefault())
        } catch (e: Throwable) {
            Log.e(TAG, e.toString())
            return ""
        }
    }
}
