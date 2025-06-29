package com.navigine.navigine.demo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.navigine.idl.java.Position
import com.navigine.idl.java.PositionListener
import com.navigine.navigine.demo.ui.activities.MainActivity
import com.navigine.navigine.demo.utils.NavigineSdkManager
import com.navigine.navigine.demo.R
import com.navigine.navigine.demo.utils.Constants

class NavigationService : Service() {
    private val mPositionListener: PositionListener? = object : PositionListener() {
        override fun onPositionUpdated(position: Position) {
            val intent: Intent = Intent(ACTION_POSITION_UPDATED)
            intent.putExtra(KEY_LOCATION_ID, position.getLocationPoint().getLocationId())
            intent.putExtra(KEY_SUBLOCATION_ID, position.getLocationPoint().getSublocationId())
            intent.putExtra(KEY_POINT_X, position.getLocationPoint().getPoint().getX())
            intent.putExtra(KEY_POINT_Y, position.getLocationPoint().getPoint().getY())
            intent.putExtra(KEY_LOCATION_HEADING, position.getLocationHeading())

            sendBroadcast(intent)
        }

        override fun onPositionError(error: Error) {
            val intent: Intent = Intent(ACTION_POSITION_ERROR)
            intent.putExtra(KEY_ERROR, error.message)
            sendBroadcast(intent)
        }
    }

    private var wakeLock: WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        wakeLockAcquire()
        addPositionListener()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        val notification = createNotification()
        startForeground(1, notification)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        removePositionListener()
        wakeLockRelease()
        super.onDestroy()
    }

    private fun addPositionListener() {
        if (NavigineSdkManager.NavigationManager != null) {
            if (mPositionListener != null) {
                NavigineSdkManager.NavigationManager.addPositionListener(mPositionListener)
            }
        }
    }

    private fun removePositionListener() {
        if (NavigineSdkManager.NavigationManager != null) {
            if (mPositionListener != null) {
                NavigineSdkManager.NavigationManager.removePositionListener(mPositionListener)
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            Constants.NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)

        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE)

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.navigation_service_name))
            .setSmallIcon(R.drawable.ic_navigation)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun wakeLockAcquire() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "navigine:wakelock")
        if (wakeLock != null) wakeLock!!.acquire()
    }

    private fun wakeLockRelease() {
        if (wakeLock != null) wakeLock!!.release()
    }

    companion object {
        @JvmField
        var INSTANCE: NavigationService? = null
        const val ACTION_POSITION_UPDATED: String = "ACTION_POSITION_UPDATED"
        const val ACTION_POSITION_ERROR: String = "ACTION_POSITION_ERROR"
        const val KEY_LOCATION_ID: String = "location_id"
        const val KEY_SUBLOCATION_ID: String = "sublocation_id"

        const val KEY_LOCATION_HEADING: String = "location_heading"
        const val KEY_POINT_X: String = "point_x"
        const val KEY_POINT_Y: String = "point_y"
        const val KEY_ERROR: String = "error"

        fun startService(context: Context?) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (context != null) {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, NavigationService::class.java)
                    )
                }
            }
        }

        fun stopService(context: Context?) {
            if (context != null) context.stopService(Intent(context, NavigationService::class.java))
        }
    }
}
