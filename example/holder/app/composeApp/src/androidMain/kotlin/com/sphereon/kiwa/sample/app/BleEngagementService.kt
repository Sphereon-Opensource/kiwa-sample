/*
 * © 2025 Sphereon International B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.sphereon.kiwa.sample.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Foreground service to maintain BLE advertising and engagement when the screen is locked.
 *
 * This service is essential for keeping BLE operations alive when the device screen is off
 * or the app is in the background. Without this service running as a foreground service,
 * Android will suspend BLE advertising when the screen locks, causing connection failures.
 *
 * Key responsibilities:
 * - Maintain foreground service status to prevent Android from killing BLE operations
 * - Display persistent notification to indicate active BLE engagement
 * - Survive screen lock and app backgrounding
 * - Coordinate with engagement manager for BLE lifecycle
 *
 * Note: This service must be started with startForeground() within 5 seconds of creation
 * as per Android requirements for foreground services.
 */
class BleEngagementService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "ble_engagement_channel"
        private const val CHANNEL_NAME = "BLE Engagement"

        /**
         * Starts the BLE engagement foreground service.
         *
         * @param context Application or activity context
         */
        fun start(context: Context) {
            val intent = Intent(context, BleEngagementService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stops the BLE engagement foreground service.
         *
         * @param context Application or activity context
         */
        fun stop(context: Context) {
            val intent = Intent(context, BleEngagementService::class.java)
            context.stopService(intent)
        }
    }

    private val app: KiwaSampleApplication
        get() = application as KiwaSampleApplication

    override fun onCreate() {
        super.onCreate()
        app.log.info("BleEngagementService: Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        app.log.info("BleEngagementService: onStartCommand called")

        val notification = createNotification()

        // Start foreground with appropriate service type for Android 14+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        app.log.info("BleEngagementService: Started as foreground service")

        // Return START_STICKY to ensure service is restarted if killed by system
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // This service doesn't support binding
        return null
    }

    override fun onDestroy() {
        app.log.info("BleEngagementService: Service destroyed")
        super.onDestroy()
    }

    /**
     * Creates the notification channel for BLE engagement notifications.
     * Required for Android O and above.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps BLE engagement active"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Creates the persistent notification shown while the service is running.
     *
     * @return Notification to be displayed
     */
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BLE Engagement Active")
            .setContentText("Ready to connect with nearby devices")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
