package com.wazuh.mobilesentinel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.wazuh.mobilesentinel.receivers.AppInstallReceiver

/**
 * Keeps the app process alive via a persistent low-priority notification.
 * Android's Background Execution Limits skip delivery of manifest-declared
 * broadcast receivers (e.g. for PACKAGE_ADDED) once an app has no active
 * process, even for package-specific broadcasts - confirmed via
 * "dumpsys activity broadcasts history-forced" showing "skipped by policy:
 * Background execution not allowed", and confirmed platform-wide (even
 * Google Play Store / GMS manifest receivers for PACKAGE_ADDED were skipped
 * the same way). Dynamically-registered receivers on an active process are
 * not subject to this restriction, so AppInstallReceiver is registered here
 * instead of in AndroidManifest.xml.
 */
class MonitoringService : Service() {

    companion object {
        private const val CHANNEL_ID = "monitoring_channel"
        private const val NOTIFICATION_ID = 1
    }

    private val appInstallReceiver = AppInstallReceiver()

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Monitoring",
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            this,
            appInstallReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(appInstallReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was never registered (e.g. onCreate didn't run) - safe to ignore.
        }
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Wazuh Sentinel")
            .setContentText("Wazuh Sentinel is monitoring device security")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
