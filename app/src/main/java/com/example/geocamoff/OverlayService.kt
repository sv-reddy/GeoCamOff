package com.example.geocamoff

import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class OverlayService : Service() {    companion object {
        private const val TAG = "OverlayService"
        private const val CHANNEL_ID = "overlay_service_channel"
        private const val NOTIFICATION_ID = 1
    }

    private var keyguardManager: KeyguardManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate called")
        
        keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isForegroundMode = intent?.getBooleanExtra("foreground_mode", false) == true
        val overlayReason = intent?.getStringExtra("overlay_reason") ?: "UNKNOWN"
        val reasonMessage = intent?.getStringExtra("reason_message") ?: "Camera access restricted"
        
        Log.d(TAG, "onStartCommand - foregroundMode: $isForegroundMode, reason: $overlayReason")
        
        if (isForegroundMode) {
            handleForegroundMode(reasonMessage)
            return START_NOT_STICKY
        }
        
        // Handle different overlay reasons
        when (overlayReason) {
            "KEYGUARD_RESTRICTED" -> handleKeyguardRestriction(reasonMessage)
            "LOCATION_DISABLED" -> handleLocationDisabled(reasonMessage)
            "RESTRICTED_ZONE" -> handleRestrictedZone(reasonMessage)
            else -> handleDefaultOverlay(reasonMessage)
        }
        
        startForeground(NOTIFICATION_ID, createNotification(reasonMessage))
        return START_STICKY
    }

    private fun handleForegroundMode(message: String) {
        Log.d(TAG, "Running in foreground mode - showing temporary notification")
        startForeground(NOTIFICATION_ID, createNotification(message))
          // Stop the service after 1 second when app is in foreground (reduced from 5 seconds)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                stopSelf()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping foreground service: ${e.message}", e)
            }
        }, 1000)
    }    private fun handleKeyguardRestriction(message: String) {
        Log.d(TAG, "Handling keyguard restriction")
        
        // Show notification for keyguard scenarios
        startForeground(NOTIFICATION_ID, createNotification(message))
    }    private fun handleLocationDisabled(message: String) {
        Log.d(TAG, "Handling location disabled scenario")
        // Show notification instead of overlay
        startForeground(NOTIFICATION_ID, createNotification(message))
    }    private fun handleRestrictedZone(message: String) {
        Log.d(TAG, "Handling restricted zone scenario")
        // Show notification instead of overlay
        startForeground(NOTIFICATION_ID, createNotification(message))
    }    private fun handleDefaultOverlay(message: String) {
        Log.d(TAG, "Handling default overlay")
        // Show notification instead of overlay
        startForeground(NOTIFICATION_ID, createNotification(message))
    }
    private fun removeOverlay() {
        // Overlay functionality removed - no overlay to remove
        Log.d(TAG, "Overlay removal skipped - overlay functionality removed")
    }    private fun createNotificationChannel() {
        // NotificationChannel is only available from API 26 (Android 8.0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Camera Restriction Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows notifications when camera access is restricted"
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(message: String): android.app.Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚫 Camera Access Restricted")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        
        removeOverlay()
        
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
            Log.d(TAG, "Foreground service stopped successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping foreground: ${e.message}", e)
        }
        
        super.onDestroy()
    }
}
