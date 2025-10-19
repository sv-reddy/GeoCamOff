package com.example.geocamoff

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class SecurityNotificationService : Service() {    companion object {
        private const val TAG = "SecurityNotification"
        
        // Default security contact (can be configured via file)
        private const val DEFAULT_SECURITY_PHONE = "+1234567890" // Replace with actual security number
        private const val DEFAULT_SECURITY_EMAIL = "security@company.com" // Replace with actual security email
        
        // Prevent notification spam - multiple layers of protection
        private var lastNotificationTime = 0L
        private var lastNotificationHash = 0 // To detect identical notifications
        private const val MIN_NOTIFICATION_INTERVAL_MS = 30000L // 30 seconds minimum between notifications
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SecurityNotificationService created")
    }    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val cameraActive = intent?.getBooleanExtra("camera_active", false) ?: false
        val locationInfo = intent?.getStringExtra("location_info") ?: ""
        val deviceInfo = intent?.getStringExtra("device_info") ?: ""
        
        Log.d(TAG, "Security notification service called - cameraActive: $cameraActive")
        
        if (cameraActive && isNotificationsEnabled()) {
            val currentTime = System.currentTimeMillis()
            val timeSinceLastNotification = currentTime - lastNotificationTime
            
            // Create a hash of the notification content to detect duplicates
            val notificationHash = (locationInfo + deviceInfo).hashCode()
            
            if (timeSinceLastNotification >= MIN_NOTIFICATION_INTERVAL_MS && notificationHash != lastNotificationHash) {
                Log.d(TAG, "Sending security alert - sufficient time elapsed and content is unique")
                sendSecurityAlert(locationInfo, deviceInfo)
                lastNotificationTime = currentTime
                lastNotificationHash = notificationHash
            } else {
                if (timeSinceLastNotification < MIN_NOTIFICATION_INTERVAL_MS) {
                    Log.d(TAG, "Security alert suppressed - too soon since last notification (${timeSinceLastNotification}ms)")
                } else {
                    Log.d(TAG, "Security alert suppressed - duplicate content detected")
                }
            }
        } else {
            Log.d(TAG, "Security alert not sent - cameraActive: $cameraActive, enabled: ${isNotificationsEnabled()}")
        }
        
        // Stop the service after handling the request
        stopSelf()
        return START_NOT_STICKY
    }
      private fun isNotificationsEnabled(): Boolean {
        return SecurityConfigManager.isNotificationsEnabled(this)
    }
    
    private fun getSecurityPhone(): String {
        return SecurityConfigManager.getSecurityPhone(this)
    }
    
    private fun getSecurityEmail(): String {
        return SecurityConfigManager.getSecurityEmail(this)
    }
    
    private fun sendSecurityAlert(locationInfo: String, deviceInfo: String) {
        thread {
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val currentLocation = getCurrentLocation()
                
                val message = buildAlertMessage(timestamp, locationInfo, deviceInfo, currentLocation)
                
                // Send SMS alert
                sendSMSAlert(message)
                
                // Send email alert (simplified - in production you'd use proper email API)
                sendEmailAlert(message)
                
                // Log the security event
                logSecurityEvent(message)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending security alert: ${e.message}", e)
            }
        }
    }
    
    private fun buildAlertMessage(timestamp: String, locationInfo: String, deviceInfo: String, currentLocation: String): String {
        return """
            🚨 SECURITY ALERT 🚨
            
            Camera accessed at: $timestamp
            
            Device Information:
            $deviceInfo
            
            Location Details:
            $locationInfo
            $currentLocation
            
            This is an automated security notification from GeoCamOff monitoring system.
        """.trimIndent()
    }
    
    private fun getCurrentLocation(): String {
        return try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            
            // Check if we have location permission and location is enabled
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                
                if (location != null) {
                    "GPS Coordinates: ${location.latitude}, ${location.longitude}"
                } else {
                    "Location: Unable to determine current coordinates"
                }
            } else {
                "Location: Permission not granted"
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting location: ${e.message}")
            "Location: Access denied"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location: ${e.message}")
            "Location: Error retrieving coordinates"
        }
    }
    
    private fun sendSMSAlert(message: String) {
        try {
            // Check if we have SMS permission
            if (checkSelfPermission(android.Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                val smsManager = SmsManager.getDefault()
                val securityPhone = getSecurityPhone()
                
                // SMS has character limit, so we might need to split the message
                val parts = smsManager.divideMessage(message)
                if (parts.size == 1) {
                    smsManager.sendTextMessage(securityPhone, null, message, null, null)
                } else {
                    smsManager.sendMultipartTextMessage(securityPhone, null, parts, null, null)
                }
                
                Log.d(TAG, "SMS alert sent to security: $securityPhone")
            } else {
                Log.w(TAG, "SMS permission not granted - cannot send SMS alert")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS alert: ${e.message}", e)
        }
    }
    
    private fun sendEmailAlert(message: String) {
        try {
            // Create an email intent
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(getSecurityEmail()))
                putExtra(Intent.EXTRA_SUBJECT, "🚨 Security Alert - Unauthorized Camera Access")
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            // Try to send email using available email clients
            val chooser = Intent.createChooser(emailIntent, "Send Security Alert")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            try {
                startActivity(chooser)
                Log.d(TAG, "Email alert intent created for security: ${getSecurityEmail()}")
            } catch (e: Exception) {
                Log.w(TAG, "No email app available to send alert: ${e.message}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating email alert: ${e.message}", e)
        }
    }
    
    private fun logSecurityEvent(message: String) {
        // Log to system log for audit trail
        Log.i(TAG, "SECURITY EVENT LOGGED: $message")
          // In a production environment, you might also:
        // - Write to a local database
        // - Send to a remote logging service
        // - Write to a local file for audit purposes
        
        try {
            // Save to file-based audit log using SecurityConfigManager
            val timestamp = System.currentTimeMillis()
            Log.d(TAG, "Security event logged at $timestamp: $message")
            
            // For now, just log to system log. In production, you could extend
            // SecurityConfigManager to handle audit logging to files
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving security event to log: ${e.message}")
        }
    }
      // Utility functions for managing security settings
    fun updateSecuritySettings(context: Context, phone: String?, email: String?, enabled: Boolean) {
        SecurityConfigManager.updateSecuritySettings(context, phone, email, enabled)
    }
    
    fun getSecuritySettings(context: Context): SecuritySettings {
        val config = SecurityConfigManager.getSecuritySettings(context)
        return SecuritySettings(
            phone = config.phone,
            email = config.email,
            enabled = config.enabled
        )
    }
    
    data class SecuritySettings(
        val phone: String,
        val email: String,
        val enabled: Boolean
    )
}
