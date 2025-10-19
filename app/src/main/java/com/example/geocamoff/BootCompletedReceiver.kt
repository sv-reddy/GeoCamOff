package com.example.geocamoff

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootCompletedReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "Device boot completed or package replaced, starting services")
                
                // Check if we have necessary permissions before starting services
                if (hasRequiredPermissions(context)) {
                    startRequiredServices(context)
                } else {
                    Log.w(TAG, "Missing required permissions, cannot start services automatically")
                    // Optionally, show a notification to user to open the app and grant permissions
                    showPermissionNotification(context)
                }
            }
        }
    }

    private fun hasRequiredPermissions(context: Context): Boolean {
        val permissions = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.SYSTEM_ALERT_WINDOW
        )
        
        return permissions.all { permission ->
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, 
                permission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startRequiredServices(context: Context) {
        try {
            // Start the camera detection service
            val cameraServiceIntent = Intent(context, CameraDetectionService::class.java)
            context.startService(cameraServiceIntent)
            Log.d(TAG, "Camera detection service started")
            
            // Note: Accessibility service will be started by the system if enabled
            Log.d(TAG, "Services started successfully after boot")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting services after boot: ${e.message}", e)
        }
    }

    private fun showPermissionNotification(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            
            // Create notification channel for Android 8.0+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    "geocamoff_permissions",
                    "GeoCamOff Permissions",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for permission requests"
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Create intent to open the app
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = android.app.PendingIntent.getActivity(
                context, 
                0, 
                openAppIntent, 
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            // Build notification
            val notification = androidx.core.app.NotificationCompat.Builder(context, "geocamoff_permissions")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("GeoCamOff Needs Permissions")
                .setContentText("Tap to open app and grant required permissions")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(1001, notification)
            Log.d(TAG, "Permission notification shown")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing permission notification: ${e.message}", e)
        }
    }
}
