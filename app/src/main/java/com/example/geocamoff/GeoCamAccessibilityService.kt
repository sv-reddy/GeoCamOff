package com.example.geocamoff

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class GeoCamAccessibilityService : AccessibilityService() {    companion object {
        private const val TAG = "GeoCamAccessibility"
        
        // Track the last active package to prevent repeated notifications for the same app
        private var lastActivePackage: String? = null
        
        private val CAMERA_PACKAGES = setOf(
            "com.android.camera",
            "com.android.camera2",
            "com.google.android.GoogleCamera",
            "com.samsung.android.camera",
            "com.huawei.camera",
            "com.oneplus.camera",
            "com.miui.camera",
            "org.codeaurora.snapcam",
            "com.motorola.camera3",
            "com.sony.playmemories.mobile",
            "com.htc.camera",
            "com.lge.camera",
            "com.asus.camera"
        )
    }    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let { accessibilityEvent ->
            when (accessibilityEvent.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    handleWindowStateChanged(accessibilityEvent)
                }
                // Removed TYPE_WINDOW_CONTENT_CHANGED handling to prevent spam notifications
                // Camera hardware detection is already handled by CameraDetectionService and MainActivity
            }
        }
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: return
        
        Log.d(TAG, "Window state changed - Package: $packageName, Class: $className")
          if (isCameraPackage(packageName)) {
            Log.d(TAG, "[ACCESSIBILITY] Camera app detected: $packageName")
            StateManager.updateCameraState(this, true)
            
            // Check if we need to show overlay based on current conditions
            checkAndShowOverlayIfNeeded()
        } else if (packageName != "com.example.geocamoff") {
            // If switching to non-camera app, update camera state to false
            Log.d(TAG, "[ACCESSIBILITY] Non-camera app detected: $packageName")
            StateManager.updateCameraState(this, false)
        }
    }    private fun isCameraPackage(packageName: String?): Boolean {
        if (packageName == null) return false
        return CAMERA_PACKAGES.any { packageName.contains(it, ignoreCase = true) } ||
               packageName.lowercase().contains("camera")
    }

    private fun checkAndShowOverlayIfNeeded() {
        // This will be called by StateManager based on the logic
        StateManager.evaluateAndTriggerOverlay(this)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Accessibility service created")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Accessibility service destroyed")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")
    }

    fun isKeyguardLocked(): Boolean {
        try {
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as android.app.KeyguardManager
            return keyguardManager.isKeyguardLocked
        } catch (e: Exception) {
            Log.e(TAG, "Error checking keyguard state: ${e.message}")
            return false
        }
    }
}
