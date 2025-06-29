package com.navigine.navigine.demo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.navigine.navigine.demo.R
import com.navigine.navigine.demo.utils.Constants

class NavigationWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        if (NavigationService.INSTANCE == null) {
            val intent = Intent(getApplicationContext(), NavigationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) getApplicationContext().startForegroundService(
                intent
            )
            else getApplicationContext().startService(intent)
        }
        return Result.success()
    }

    override fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        return ForegroundInfo(1, createNotification())
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            Constants.NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager =
            getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(
            getApplicationContext(),
            Constants.NOTIFICATION_CHANNEL_ID
        )
            .setContentTitle(getApplicationContext().getString(R.string.navigation_service_name))
            .setSmallIcon(R.drawable.ic_navigation)
            .build()
    }
}
