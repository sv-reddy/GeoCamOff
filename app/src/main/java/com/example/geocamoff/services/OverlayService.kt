package com.example.geocamoff.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.geocamoff.MainActivity // For notification tap
import com.example.geocamoff.R
import com.example.geocamoff.data.Geofence
import com.example.geocamoff.utils.Constants
import com.example.geocamoff.utils.LocationUtils
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var cameraManager: CameraManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private var activeGeofences: MutableList<Geofence> = mutableListOf()
    private lateinit var sharedPreferences: SharedPreferences  
    private val gson = Gson()
    private var isCameraInUse: Boolean = false
    
    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            super.onCameraAvailable(cameraId)
            Log.d("OverlayService", "Camera $cameraId available")
            isCameraInUse = false
            checkLocationAndShowOverlay()
        }

        override fun onCameraUnavailable(cameraId: String) {
            super.onCameraUnavailable(cameraId)
            Log.d("OverlayService", "Camera $cameraId unavailable (in use)")
            isCameraInUse = true
            checkLocationAndShowOverlay()
            Log.d("OverlayService", "Checking location after camera became unavailable")
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            currentLocation = locationResult.lastLocation
            Log.d("OverlayService", "Location Update: ${currentLocation?.latitude}, ${currentLocation?.longitude}")
            // If an overlay is already showing due to camera use,
            // we might want to re-evaluate its message if location changed significantly
            if (overlayView?.isAttachedToWindow == true) {
                checkLocationAndShowOverlay() // Re-evaluate message
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)

        loadGeofences()
        startForegroundServiceNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Constants.ACTION_START_SERVICE -> {
                Log.d("OverlayService", "Service Started")
                registerCameraCallback()
                startLocationUpdates()
            }
            Constants.ACTION_STOP_SERVICE -> {
                Log.d("OverlayService", "Service Stopping")
                stopSelf() // This will trigger onDestroy
            }
            Constants.ACTION_UPDATE_GEOFENCES -> {
                Log.d("OverlayService", "Updating geofences")
                loadGeofences() // Reload from SharedPreferences
                // If overlay is active, re-evaluate based on new geofences
                if (overlayView?.isAttachedToWindow == true) {
                    checkLocationAndShowOverlay()
                }
            }
        }
        // If geofences are passed via intent (alternative to SharedPreferences for immediate update)
        intent?.getParcelableArrayListExtra<Geofence>("geofences_extra")?.let {
            activeGeofences.clear()
            activeGeofences.addAll(it)
            Log.d("OverlayService", "Geofences updated via Intent extra")
        }

        return START_STICKY
    }

    private fun loadGeofences() {
        val geofencesJson = sharedPreferences.getString(Constants.PREF_KEY_GEOFENCES, null)
        if (geofencesJson != null) {
            val type = object : TypeToken<MutableList<Geofence>>() {}.type
            activeGeofences = gson.fromJson(geofencesJson, type)
            Log.d("OverlayService", "Loaded ${activeGeofences.size} geofences.")
        } else {
            activeGeofences.clear()
            Log.d("OverlayService", "No geofences found in prefs.")
        }
    }

    private fun updateCameraState() {
        try {
            // Check if any camera is in use
            val cameraList = cameraManager.cameraIdList
            isCameraInUse = cameraList.any { cameraId ->
                try {
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    // We mainly care about back and front cameras
                    (facing == CameraCharacteristics.LENS_FACING_BACK ||
                            facing == CameraCharacteristics.LENS_FACING_FRONT)
                } catch (e: Exception) {
                    Log.e("OverlayService", "Error checking camera $cameraId", e)
                    false
                }
            }

            // Broadcast camera state
            val intent = Intent("com.example.geocamoff.CAMERA_STATE").apply {
                putExtra("camera_in_use", isCameraInUse)
            }
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
                .sendBroadcast(intent)

            if (!isCameraInUse) {
                removeOverlay()
            }
        } catch (e: Exception) {
            Log.e("OverlayService", "Error updating camera state", e)
        }
    }

    private fun registerCameraCallback() {
        try {
            // First check current state
            updateCameraState()
            // Then register for updates
            cameraManager.registerAvailabilityCallback(cameraCallback, null)
            // Register for each camera specifically
            cameraManager.cameraIdList.forEach { cameraId ->
                try {
                    Log.d("OverlayService", "Registering callback for camera: $cameraId")
                    cameraManager.registerAvailabilityCallback(cameraCallback, null)
                } catch (e: Exception) {
                    Log.e("OverlayService", "Error registering callback for camera $cameraId", e)
                }
            }
        } catch (e: Exception) {
            Log.e("OverlayService", "Error registering camera callback", e)
        }
    }    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("OverlayService", "Location permission not granted for service.")
            return
        }
        val locationRequest = LocationRequest.create().apply {
            interval = 5000 // 5 seconds
            fastestInterval = 3000 // 3 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 10f // 10 meters minimum movement
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }    private fun checkLocationAndShowOverlay() {
        if (!isCameraInUse) {
            removeOverlay()
            return
        }

        // Default message when camera is active
        var message = "Camera is ACTIVE!"
        var inRestrictedZone = false

        // Update message based on location and geofence status
        currentLocation?.let { loc ->
            Log.d("OverlayService", "Checking location: ${loc.latitude}, ${loc.longitude}")
            for (geofence in activeGeofences) {
                Log.d("OverlayService", "Checking geofence: ${geofence.name} at ${geofence.latitude}, ${geofence.longitude}")
                if (LocationUtils.isInsideGeofence(loc, geofence)) {
                    message = "${geofence.alertMessage}\nLocation: ${geofence.name}"
                    inRestrictedZone = true
                    Log.d("OverlayService", "Inside restricted zone: ${geofence.name}")
                    break
                }
            }

            if (!inRestrictedZone && activeGeofences.isNotEmpty()) {
                // Find nearest geofence if not in any
                val nearestGeofence = activeGeofences.minByOrNull { geofence ->
                    LocationUtils.calculateDistance(
                        loc.latitude, loc.longitude,
                        geofence.latitude, geofence.longitude
                    )
                }
                nearestGeofence?.let { nearest ->
                    val distance = LocationUtils.calculateDistance(
                        loc.latitude, loc.longitude,
                        nearest.latitude, nearest.longitude
                    )
                    message = "Camera is ACTIVE!\nNearest restricted zone: ${nearest.name} (${distance.toInt()}m away)"
                }
            }
        } ?: run {
            message = "Camera is ACTIVE!\n(Location services unavailable)"
        }

        if (activeGeofences.isEmpty()) {
            message = "Camera is ACTIVE\n(No restricted zones configured)"
        }

        displayOverlay(message)
    }

    private fun displayOverlay(message: String) {
        if (overlayView?.isAttachedToWindow == true) {
            // Update existing overlay message
            overlayView?.findViewById<TextView>(R.id.textViewOverlayMessage)?.text = message
            return
        }

        // Create new overlay
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null)
        overlayView?.findViewById<TextView>(R.id.textViewOverlayMessage)?.text = message

        val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            // Ensure overlay appears above other windows
            flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
                Log.e("OverlayService", "Cannot draw overlay, SYSTEM_ALERT_WINDOW permission not granted")
                return
            }
            windowManager.addView(overlayView, params)
            Log.d("OverlayService", "Overlay displayed with message: $message")
        } catch (e: Exception) {
            Log.e("OverlayService", "Error displaying overlay", e)
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            if (it.isAttachedToWindow) { // Check if the view is actually added
                try {
                    windowManager.removeView(it)
                    Log.d("OverlayService","Overlay removed")
                } catch (e: Exception) {
                    Log.e("OverlayService", "Error removing overlay view", e)
                }
            }
            overlayView = null
        }
    }

    private fun startForegroundServiceNotification() {
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(Constants.NOTIFICATION_CHANNEL_ID, "GeoCamOff Service Channel")
        } else {
            "" // Empty for pre-O
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)


        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("GeoCamOff Active")
            .setContentText("Monitoring camera and location.")
            .setSmallIcon(android.R.drawable.ic_menu_camera) // Use built-in icon
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Makes it non-dismissible by swipe
            .build()

        startForeground(Constants.NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(channelId: String, channelName: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(chan)
        }
        return channelId
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("OverlayService", "Service Destroyed")
        try {
            cameraManager.unregisterAvailabilityCallback(cameraCallback)
        } catch (e: Exception) {
            Log.e("OverlayService", "Error unregistering camera callback", e)
        }
        fusedLocationClient.removeLocationUpdates(locationCallback)
        removeOverlay()
        stopForeground(true) // Also remove notification
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not a bound service
    }
}