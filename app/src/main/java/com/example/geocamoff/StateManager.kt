package com.example.geocamoff

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.util.Log

object StateManager {
    private const val TAG = "StateManager"
    
    private var isInRestrictedZone = false
    private var isCameraActive = false
    private var isAppInForeground = false
    private var isLocationEnabled = false
    private var isKeyguardLocked = false
    
    // Add cooldown mechanism to prevent spam notifications
    private var lastSecurityNotificationTime = 0L
    private const val SECURITY_NOTIFICATION_COOLDOWN_MS = 60000L // 1 minute cooldown
    
    // Add synchronization to prevent concurrent state updates
    private val stateLock = Any()
    
    // Track the last camera state change to prevent duplicate processing from multiple sources
    private var lastCameraStateChangeTime = 0L
    private const val CAMERA_STATE_CHANGE_DEBOUNCE_MS = 1000L // 1 second debounce

    fun setAppForegroundState(inForeground: Boolean) {
        isAppInForeground = inForeground
        Log.d(TAG, "App foreground state changed: $inForeground")
    }

    fun updateGeofenceState(context: Context, inRestrictedZone: Boolean) {
        isInRestrictedZone = inRestrictedZone
        Log.d(TAG, "Geofence state changed: inRestrictedZone = $inRestrictedZone")
        evaluateAndTriggerOverlay(context)
    }    fun updateCameraState(context: Context, cameraActive: Boolean) {
        synchronized(stateLock) {
            val previousState = isCameraActive
            val currentTime = System.currentTimeMillis()
            val sourceName = when {
                context::class.java.simpleName.contains("MainActivity") -> "FOREGROUND"
                context::class.java.simpleName.contains("CameraDetectionService") -> "SERVICE"
                context::class.java.simpleName.contains("GeoCamAccessibilityService") -> "ACCESSIBILITY"
                else -> "UNKNOWN"
            }
            
            // Check if this is actually a state change to prevent duplicate processing
            if (previousState == cameraActive) {
                Log.d(TAG, "[$sourceName] Camera state update ignored - no change (already $cameraActive)")
                return
            }
            
            // Debounce rapid state changes from multiple detection sources
            val timeSinceLastChange = currentTime - lastCameraStateChangeTime
            if (timeSinceLastChange < CAMERA_STATE_CHANGE_DEBOUNCE_MS) {
                Log.d(TAG, "[$sourceName] Camera state update ignored - too soon since last change (${timeSinceLastChange}ms)")
                return
            }
            
            isCameraActive = cameraActive
            lastCameraStateChangeTime = currentTime
              Log.d(TAG, "[$sourceName] Camera state update: previous=$previousState, new=$cameraActive")
            
            // Send security notification only when camera becomes active AND user is in a violation state
            if (cameraActive) {
                // Update location state to get current status
                updateLocationStateFromSystem(context)
                
                // Check if this camera activation should trigger a security notification
                val shouldNotifySecurity = !isLocationEnabled || isInRestrictedZone
                
                if (shouldNotifySecurity) {
                    val timeSinceLastNotification = currentTime - lastSecurityNotificationTime
                    
                    if (timeSinceLastNotification >= SECURITY_NOTIFICATION_COOLDOWN_MS) {
                        val violationType = if (!isLocationEnabled) "LOCATION_DISABLED" else "RESTRICTED_ZONE"
                        Log.d(TAG, "[$sourceName] Sending security notification - camera active in violation state: $violationType")
                        sendSecurityNotification(context)
                        lastSecurityNotificationTime = currentTime
                    } else {
                        Log.d(TAG, "[$sourceName] Security notification suppressed - cooldown active (${timeSinceLastNotification}ms since last)")
                    }
                } else {
                    Log.d(TAG, "[$sourceName] No security notification - camera active in allowed area (location enabled, not in restricted zone)")
                }
            } else {
                Log.d(TAG, "[$sourceName] No security notification needed - camera became inactive")
            }
            
            evaluateAndTriggerOverlay(context)
        }
    }

    fun updateLocationState(context: Context, locationEnabled: Boolean) {
        isLocationEnabled = locationEnabled
        Log.d(TAG, "Location state changed: locationEnabled = $locationEnabled")
        evaluateAndTriggerOverlay(context)
    }    // Note: This function is currently unused but kept for future keyguard detection implementation
    // It's referenced by the overlay logic for KEYGUARD_RESTRICTED scenario
    @Suppress("unused")
    fun updateKeyguardState(context: Context, keyguardLocked: Boolean) {
        isKeyguardLocked = keyguardLocked
        Log.d(TAG, "Keyguard state changed: keyguardLocked = $keyguardLocked")
        evaluateAndTriggerOverlay(context)
    }

    fun evaluateAndTriggerOverlay(context: Context) {
        // Update location state dynamically
        updateLocationStateFromSystem(context)
        
        val overlayReason = determineOverlayReason()
        val shouldShowOverlay = overlayReason != null
        
        Log.d(TAG, "Overlay evaluation - shouldShow: $shouldShowOverlay, reason: $overlayReason")
        Log.d(TAG, "Current state - Camera: $isCameraActive, Location: $isLocationEnabled, InRestrictedZone: $isInRestrictedZone, Keyguard: $isKeyguardLocked")
        
        if (shouldShowOverlay) {
            showOverlay(context, overlayReason!!)
        } else {
            hideOverlay(context)
        }
    }

    private fun updateLocationStateFromSystem(context: Context) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            isLocationEnabled = isGpsEnabled || isNetworkEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Error checking location state: ${e.message}")
        }
    }

    private fun determineOverlayReason(): OverlayReason? {
        // Only show overlay if camera is active
        if (!isCameraActive) return null

        return when {
            // Rule 1: Location is OFF and camera is opened
            !isLocationEnabled -> OverlayReason.LOCATION_DISABLED
            
            // Rule 2: Location is ON and user is in restricted zone
            isLocationEnabled && isInRestrictedZone -> OverlayReason.RESTRICTED_ZONE
            
            // Rule 3: Camera access from lock screen with restrictions
            isKeyguardLocked -> OverlayReason.KEYGUARD_RESTRICTED
            
            // Rule 4: Location is ON and NOT in restricted zone - Allow camera use
            else -> null
        }
    }

    private fun showOverlay(context: Context, reason: OverlayReason) {
        // Don't show overlay if app is already in foreground (unless it's a keyguard restriction)
        if (isAppInForeground && reason != OverlayReason.KEYGUARD_RESTRICTED) {
            Log.d(TAG, "App in foreground, showing notification instead of overlay")
            showNotificationInstead(context, reason)
            return
        }
        
        val overlayIntent = Intent(context, OverlayService::class.java).apply {
            putExtra("overlay_reason", reason.name)
            putExtra("reason_message", reason.getMessage())
        }
        
        if (Build.VERSION.SDK_INT >= 34) {
            // On Android 14+, bring app to foreground before starting overlay
            val activityIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("start_overlay", true)
                putExtra("overlay_reason", reason.name)
            }
            context.startActivity(activityIntent)
        } else {
            context.startService(overlayIntent)
        }
    }

    private fun hideOverlay(context: Context) {
        try {
            val overlayIntent = Intent(context, OverlayService::class.java)
            context.stopService(overlayIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping overlay service: ${e.message}")
        }
    }

    private fun showNotificationInstead(context: Context, reason: OverlayReason) {        val notificationIntent = Intent(context, OverlayService::class.java).apply {
            putExtra("foreground_mode", true)
            putExtra("overlay_reason", reason.name)
            putExtra("reason_message", reason.getMessage())
        }
        
        context.startService(notificationIntent)
    }

    private fun sendSecurityNotification(context: Context) {
        try {
            val locationInfo = if (isLocationEnabled) {
                if (isInRestrictedZone) "User is in a RESTRICTED ZONE" else "User is in an allowed area"
            } else {
                "Location services are DISABLED"
            }
              val deviceInfo = "Device: ${Build.MODEL} (${Build.MANUFACTURER})\n" +
                           "Android: ${Build.VERSION.RELEASE}\n" +
                           "App State: ${if (isAppInForeground) "Foreground" else "Background"}\n" +
                           "Screen: ${if (isKeyguardLocked) "Locked" else "Unlocked"}"
            
            val securityIntent = Intent(context, SecurityNotificationService::class.java).apply {
                putExtra("camera_active", true)
                putExtra("location_info", locationInfo)
                putExtra("device_info", deviceInfo)
            }
            
            context.startService(securityIntent)
            Log.d(TAG, "Security notification triggered - camera activated")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering security notification: ${e.message}", e)
        }
    }

    enum class OverlayReason {
        LOCATION_DISABLED,
        RESTRICTED_ZONE,
        KEYGUARD_RESTRICTED;

        fun getMessage(): String = when (this) {
            LOCATION_DISABLED -> "Camera access blocked: Location services are disabled. Please enable location to use camera."
            RESTRICTED_ZONE -> "Camera access blocked: You are in a restricted area where camera usage is not allowed."
            KEYGUARD_RESTRICTED -> "Camera access from lock screen is restricted in this area."
        }
    }

    // Getters for current state (useful for UI updates)
    fun getCurrentState() = StateInfo(
        isInRestrictedZone = isInRestrictedZone,
        isCameraActive = isCameraActive,
        isAppInForeground = isAppInForeground,
        isLocationEnabled = isLocationEnabled,
        isKeyguardLocked = isKeyguardLocked
    )

    data class StateInfo(
        val isInRestrictedZone: Boolean,
        val isCameraActive: Boolean,
        val isAppInForeground: Boolean,
        val isLocationEnabled: Boolean,
        val isKeyguardLocked: Boolean
    )
}
