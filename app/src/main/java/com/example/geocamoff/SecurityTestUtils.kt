package com.example.geocamoff

import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Test utility for demonstrating the security notification system
 */
object SecurityTestUtils {
    private const val TAG = "SecurityTestUtils"
    
    /**
     * Simulate camera activation for testing security notifications
     */
    fun testSecurityNotification(context: Context) {
        Log.d(TAG, "Testing security notification system...")
        
        try {
            val locationInfo = "TEST MODE: Simulated camera access"
            val deviceInfo = "Device: ${android.os.Build.MODEL} (${android.os.Build.MANUFACTURER})\n" +
                           "Android: ${android.os.Build.VERSION.RELEASE}\n" +
                           "App State: Testing\n" +
                           "Screen: Unlocked"
            
            val securityIntent = Intent(context, SecurityNotificationService::class.java).apply {
                putExtra("camera_active", true)
                putExtra("location_info", locationInfo)
                putExtra("device_info", deviceInfo)
            }
            
            context.startService(securityIntent)
            Log.d(TAG, "Test security notification triggered successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during security notification test: ${e.message}", e)
        }
    }
      /**
     * Check if security notifications are properly configured
     */
    fun checkSecurityConfiguration(context: Context): SecurityCheckResult {
        val settings = SecurityConfigManager.getSecuritySettings(context)
        
        val issues = mutableListOf<String>()
        
        // Check if notifications are enabled
        if (!settings.enabled) {
            issues.add("Security notifications are disabled")
        }
        
        // Check if at least one contact method is configured
        if (settings.phone.isEmpty() && settings.email.isEmpty()) {
            issues.add("No security contact information configured")
        }
        
        // Check SMS permission
        val smsPermission = context.checkSelfPermission(android.Manifest.permission.SEND_SMS)
        if (smsPermission != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            issues.add("SMS permission not granted")
        }
        
        return SecurityCheckResult(
            isConfigured = issues.isEmpty(),
            issues = issues,
            settings = SecurityNotificationService.SecuritySettings(
                phone = settings.phone,
                email = settings.email,
                enabled = settings.enabled
            )
        )
    }
    
    data class SecurityCheckResult(
        val isConfigured: Boolean,
        val issues: List<String>,
        val settings: SecurityNotificationService.SecuritySettings
    )
}
