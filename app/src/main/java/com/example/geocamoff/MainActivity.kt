package com.example.geocamoff

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 100
    }

    private lateinit var startActivityForResultLauncher: ActivityResultLauncher<Intent>
    private var cameraManager: CameraManager? = null
    private var foregroundCameraCallback: CameraManager.AvailabilityCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the activity result launcher
        startActivityForResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()        ) { _ ->
            // Handle permission results if needed
            requestRequiredPermissions()
        }
        setupNavigation()
        showPermissionDialogIfNeeded()
        // Add a delay and recheck permissions (for debugging)
        findViewById<View>(android.R.id.content).postDelayed({
            checkAllPermissions()
        }, 2000)
    }    private fun checkAllPermissions() {
        val camera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val location = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else true
        val sms = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

        Log.d("MainActivity", "=== Current Permission Status ===")
        Log.d("MainActivity", "Camera: $camera")
        Log.d("MainActivity", "Location: $location")
        Log.d("MainActivity", "Notification: $notification")
        Log.d("MainActivity", "Background Location: $backgroundLocation")
        Log.d("MainActivity", "SMS: $sms")
        Log.d("MainActivity", "All granted: ${camera && location && notification && backgroundLocation && sms}")
    }    private fun showPermissionDialogIfNeeded() {
        val needsCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        val needsLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        val needsSms = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED

        // Check additional permissions based on Android version
        val needsNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        } else false

        // Background location can only be requested if fine location is already granted
        val needsBackgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !needsLocation) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
        } else false        // Debug logging
        Log.d("MainActivity", "Permission Status:")
        Log.d("MainActivity", "Camera: ${if (needsCamera) "MISSING" else "GRANTED"}")
        Log.d("MainActivity", "Location: ${if (needsLocation) "MISSING" else "GRANTED"}")
        Log.d("MainActivity", "Notification: ${if (needsNotification) "MISSING" else "GRANTED"}")
        Log.d("MainActivity", "Background Location: ${if (needsBackgroundLocation) "MISSING" else "GRANTED"}")
        Log.d("MainActivity", "SMS: ${if (needsSms) "MISSING" else "GRANTED"}")

        // If all essential permissions are granted, start the service directly
        // Note: Background location and SMS are not essential for basic functionality
        if (!needsCamera && !needsLocation && !needsNotification && !needsSms) {
            Log.d("MainActivity", "All essential permissions granted, starting service")
            startCameraDetectionService()

            // Request background location separately if needed
            if (needsBackgroundLocation) {
                requestBackgroundLocationLater()
            }
            return
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_permission_request, null)
        val message = dialogView.findViewById<TextView>(R.id.permission_message)
        val btnGrant = dialogView.findViewById<Button>(R.id.btn_grant)
        val needed = mutableListOf<String>()
        if (needsCamera) needed.add("Camera")
        if (needsLocation) needed.add("Location")
        if (needsNotification) needed.add("Notifications")
        if (needsSms) needed.add("SMS (for security alerts)")

        message.text = getString(R.string.permission_dialog_message, needed.joinToString("\n"))        // Show toast with missing permissions for debugging
        Toast.makeText(this, "Missing permissions: ${needed.joinToString(", ")}", Toast.LENGTH_LONG).show()
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
            
        btnGrant.setOnClickListener {
            dialog.dismiss()
            requestRequiredPermissions()
        }

        dialog.show()
    }

    private fun setupNavigation() {
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_status -> {
                    openFragment(StatusFragment())
                    true
                }
                R.id.navigation_settings -> {
                    openFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
        // Default fragment
        openFragment(StatusFragment())
    }    private fun requestRequiredPermissions() {        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.SEND_SMS
        )

        // Add Android 14+ specific permissions
        if (Build.VERSION.SDK_INT >= 34) {
            permissions.add("android.permission.FOREGROUND_SERVICE_CAMERA")
            permissions.add("android.permission.FOREGROUND_SERVICE_LOCATION")
        }

        // Add notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Add system alert window permission check (but request separately)
        checkSystemAlertWindowPermission()

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            Log.d("MainActivity", "Requesting permissions: ${notGranted.joinToString(", ")}")
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        } else {
            Log.d("MainActivity", "All basic permissions granted, starting services")
            startAllServices()
            // Request background location separately if needed
            requestBackgroundLocationLater()
        }
    }

    private fun checkSystemAlertWindowPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                // Show dialog explaining why we need this permission
                AlertDialog.Builder(this)
                    .setTitle("Overlay Permission Required")
                    .setMessage("GeoCamOff needs permission to display overlays over other apps to block camera access when in restricted zones.")
                    .setPositiveButton("Grant Permission") { _, _ ->
                        val intent = Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:$packageName")
                        )
                        startActivityForResultLauncher.launch(intent)
                    }
                    .setNegativeButton("Skip") { _, _ ->
                        Toast.makeText(this, "Overlay permission skipped - some features may not work", Toast.LENGTH_LONG).show()
                    }
                    .show()
            }
        }
    }

    private fun checkAccessibilityServiceEnabled() {
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        if (!accessibilityEnabled) {
            AlertDialog.Builder(this)
                .setTitle("Accessibility Service Required")
                .setMessage("GeoCamOff needs accessibility service access to monitor camera usage across all apps. This helps detect when camera apps are opened.")
                .setPositiveButton("Enable Service") { _, _ ->
                    val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                    Toast.makeText(this, "Please find 'GeoCamOff' in the list and enable it", Toast.LENGTH_LONG).show()
                }
                .setNegativeButton("Skip") { _, _ ->
                    Toast.makeText(this, "Accessibility service skipped - camera detection may be limited", Toast.LENGTH_LONG).show()
                }
                .show()
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = "$packageName/${GeoCamAccessibilityService::class.java.canonicalName}"
        val enabledServicesSetting = android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        
        return enabledServicesSetting.contains(expectedComponentName)
    }

    private fun startAllServices() {
        // Start camera detection service
        startService(Intent(this, CameraDetectionService::class.java))
        
        // Start location monitoring service
        startService(Intent(this, LocationMonitoringService::class.java))
        
        // Check for accessibility service
        checkAccessibilityServiceEnabled()
    }    private fun startCameraDetectionService() {
        startAllServices()
    }

    private fun requestBackgroundLocationLater() {
        // Request background location permission after a delay to ensure foreground location is established
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            findViewById<View>(android.R.id.content).postDelayed({
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        REQUEST_CODE_PERMISSIONS + 1
                    )
                }
            }, 3000) // Wait 3 seconds before requesting background location
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {            REQUEST_CODE_PERMISSIONS -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Log.d("MainActivity", "All basic permissions granted")
                    startAllServices()
                    // Request background location separately
                    requestBackgroundLocationLater()
                } else {
                    // Show rationale and re-request if denied
                    val denied = permissions.zip(grantResults.toTypedArray())
                        .filter { it.second != PackageManager.PERMISSION_GRANTED }
                        .map { it.first }
                    val rationale = denied.joinToString(", ") { perm ->
                        when (perm) {
                            Manifest.permission.CAMERA -> "Camera"
                            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION -> "Location"
                            Manifest.permission.POST_NOTIFICATIONS -> "Notifications"
                            Manifest.permission.SEND_SMS -> "SMS (for security alerts)"
                            else -> perm
                        }
                    }
                    AlertDialog.Builder(this)
                        .setTitle("Permissions Required")
                        .setMessage("The following permissions are required for the app to work properly: $rationale.\nPlease grant them in the next steps.")
                        .setCancelable(false)
                        .setPositiveButton("Grant") { _, _ -> requestRequiredPermissions() }
                        .setNegativeButton("Exit") { _, _ -> finish() }
                        .show()
                }
            }
            REQUEST_CODE_PERMISSIONS + 1 -> {
                // Background location permission result
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MainActivity", "Background location permission granted")
                    Toast.makeText(this, "Background location access granted", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("MainActivity", "Background location permission denied")
                    Toast.makeText(this, "Background location denied - some features may be limited", Toast.LENGTH_LONG).show()
                }
            }
        }    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume() - stopping camera detection service and starting foreground detection")
        
        // Notify StateManager that app is in foreground
        StateManager.setAppForegroundState(true)
        
        // Stop camera detection service when app is in foreground
        try {
            val stopIntent = Intent(this, CameraDetectionService::class.java).apply {
                action = "STOP_SERVICE"
            }
            startService(stopIntent)
        } catch (e: Exception) {
            Log.w("MainActivity", "Error stopping CameraDetectionService: ${e.message}", e)
        }
        
        // Start foreground camera detection
        startForegroundCameraDetection()
        
        if (intent?.getBooleanExtra("start_overlay", false) == true) {
            try {
                startService(Intent(this, OverlayService::class.java))
            } catch (e: Exception) {
                Log.e("MainActivity", "Error starting OverlayService: ${e.message}", e)
            }
            intent.removeExtra("start_overlay")
        }
    }    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "onPause() - stopping foreground detection and starting camera detection service")
        
        // Stop foreground camera detection
        stopForegroundCameraDetection()
        
        // Notify StateManager that app is going to background
        StateManager.setAppForegroundState(false)
          // Start camera detection when app goes to background, if permissions are granted
        val camera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val location = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        val sms = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        
          if (camera && location && notification) {
            try {
                Log.d("MainActivity", "Starting services (SMS permission: $sms)")
                startAllServices()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error starting services: ${e.message}", e)
            }
        } else {Log.d("MainActivity", "Not starting service - missing permissions. Camera: $camera, Location: $location, Notification: $notification, SMS: $sms")
        }
    }    private fun startForegroundCameraDetection() {        try {
            cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
            foregroundCameraCallback = object : CameraManager.AvailabilityCallback() {                override fun onCameraUnavailable(cameraId: String) {
                    super.onCameraUnavailable(cameraId)
                    Log.d("MainActivity", "[FOREGROUND] Camera $cameraId unavailable (in use)")
                    StateManager.updateCameraState(this@MainActivity, true)
                }
                
                override fun onCameraAvailable(cameraId: String) {
                    super.onCameraAvailable(cameraId)
                    Log.d("MainActivity", "[FOREGROUND] Camera $cameraId available (not in use)")
                    StateManager.updateCameraState(this@MainActivity, false)
                }
            }
            
            foregroundCameraCallback?.let { 
                cameraManager?.registerAvailabilityCallback(it, null)
                Log.d("MainActivity", "Foreground camera detection started")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up foreground camera detection: ${e.message}", e)
        }
    }
      private fun stopForegroundCameraDetection() {
        try {
            foregroundCameraCallback?.let { callback ->
                cameraManager?.unregisterAvailabilityCallback(callback)
                Log.d("MainActivity", "Foreground camera detection stopped")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error stopping foreground camera detection: ${e.message}", e)
        } finally {
            foregroundCameraCallback = null
            cameraManager = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForegroundCameraDetection()
    }
}