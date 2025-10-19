package com.example.geocamoff

import android.app.Service
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.os.IBinder
import android.util.Log

class CameraDetectionService : Service() {
    private var cameraManager: CameraManager? = null
    private var callback: CameraManager.AvailabilityCallback? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("CameraDetection", "Service onStartCommand() called")
        
        // Check if this is a stop command
        if (intent?.action == "STOP_SERVICE") {
            Log.d("CameraDetection", "Received stop command, stopping self")
            stopSelf()
            return START_NOT_STICKY
        }
        
        return START_NOT_STICKY // Don't restart if killed
    }    override fun onCreate() {
        super.onCreate()
        Log.d("CameraDetection", "Service onCreate() called")
          try {
            cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
            
            callback = object : CameraManager.AvailabilityCallback() {                override fun onCameraUnavailable(cameraId: String) {
                    super.onCameraUnavailable(cameraId)
                    Log.d("CameraDetection", "[SERVICE] Camera $cameraId unavailable (in use) - triggering security check")
                    StateManager.updateCameraState(this@CameraDetectionService, true)
                }
                
                override fun onCameraAvailable(cameraId: String) {
                    super.onCameraAvailable(cameraId)
                    Log.d("CameraDetection", "[SERVICE] Camera $cameraId available (not in use) - camera released")
                    StateManager.updateCameraState(this@CameraDetectionService, false)
                }
            }
            
            callback?.let { 
                cameraManager?.registerAvailabilityCallback(it, null)
                Log.d("CameraDetection", "Camera callback registered successfully")
            }
        } catch (e: Exception) {
            Log.e("CameraDetection", "Error setting up camera detection: ${e.message}", e)
        }
    }    override fun onDestroy() {
        Log.d("CameraDetection", "Service onDestroy() called")
        
        try {
            callback?.let { cb ->
                cameraManager?.unregisterAvailabilityCallback(cb)
                Log.d("CameraDetection", "Camera callback unregistered successfully")
            }
        } catch (e: Exception) {
            Log.e("CameraDetection", "Error during service cleanup: ${e.message}", e)
        } finally {
            callback = null
            cameraManager = null
        }
        
        super.onDestroy()
    }
}
