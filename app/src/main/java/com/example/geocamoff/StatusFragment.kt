package com.example.geocamoff

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*

class StatusFragment : Fragment() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var statusText: TextView
    private val updateHandler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_status, container, false)
        statusText = view.findViewById(R.id.status_text)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        startStatusUpdates()
        return view
    }

    private fun startStatusUpdates() {
        updateRunnable = object : Runnable {
            override fun run() {
                if (isAdded) {
                    updateStatus()
                    updateHandler.postDelayed(this, 500) // Update every 0.5 seconds (reduced from 1 second)
                }
            }
        }
        updateRunnable?.let { updateHandler.post(it) }
    }

    private fun updateStatus() {
        if (!isAdded) return
        
        val context = requireContext()
        val stateInfo = StateManager.getCurrentState()
        
        // Check all permissions and services
        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasOverlay = Settings.canDrawOverlays(context)
        val hasAccessibility = isAccessibilityServiceEnabled()
        val isLocationEnabled = isLocationServiceEnabled()
        
        // Build status text
        val statusBuilder = StringBuilder()
        statusBuilder.append("🔒 GeoCamOff Status\n\n")
        
        // System Status
        statusBuilder.append("🌍 SYSTEM STATUS:\n")
        statusBuilder.append("Location Service: ${if (isLocationEnabled) "✅ Enabled" else "❌ Disabled"}\n")
        statusBuilder.append("Camera Active: ${if (stateInfo.isCameraActive) "🔴 Yes" else "✅ No"}\n")
        statusBuilder.append("In Restricted Zone: ${if (stateInfo.isInRestrictedZone) "🚫 Yes" else "✅ No"}\n\n")
        
        // Add current location if available
        if (hasLocation) {
            requestLocationUpdate(statusBuilder.toString())
        } else {
            statusText.text = statusBuilder.toString()
        }
    }    private fun requestLocationUpdate(baseStatus: String) {
        if (!isAdded) return
        
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            statusText.text = "$baseStatus\n\n📍 LOCATION: Permission not granted"
            return
        }
        
        // Request fresh location with high accuracy for immediate updates
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500).build()
        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (!isAdded) return
                
                val location = result.lastLocation
                val locationStatus = if (location != null) {
                    val currentPoint = LatLngPoint(location.latitude, location.longitude)
                    val polygonGeofences = RestrictedAreaLoader.loadRestrictedAreas(requireContext())
                    val inside = polygonGeofences.firstOrNull { geofence ->
                        PolygonGeofenceUtils.isInsidePolygonGeofence(currentPoint, geofence)
                    }
                    
                    // Update StateManager immediately
                    StateManager.updateGeofenceState(requireContext(), inside != null)
                    
                    if (inside != null) {
                        "📍 LOCATION: ${location.latitude}, ${location.longitude}\n🚩 ZONE: ${inside.name} (RESTRICTED)"
                    } else {
                        "📍 LOCATION: ${location.latitude}, ${location.longitude}\n✅ ZONE: Safe Area"
                    }
                } else {
                    "📍 LOCATION: Unable to get current location"
                }
                
                statusText.text = "$baseStatus\n\n$locationStatus"
                
                // Remove location updates after getting result to save battery
                fusedLocationClient.removeLocationUpdates(this)
            }
        }, Looper.getMainLooper())
        
        // Fallback to last known location if real-time request fails
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (!isAdded) return@addOnSuccessListener
            
            val locationStatus = if (location != null) {
                val currentPoint = LatLngPoint(location.latitude, location.longitude)
                val polygonGeofences = RestrictedAreaLoader.loadRestrictedAreas(requireContext())
                val inside = polygonGeofences.firstOrNull { geofence ->
                    PolygonGeofenceUtils.isInsidePolygonGeofence(currentPoint, geofence)
                }
                
                // Update StateManager
                StateManager.updateGeofenceState(requireContext(), inside != null)
                
                if (inside != null) {
                    "📍 LOCATION: ${location.latitude}, ${location.longitude}\n🚩 ZONE: ${inside.name} (RESTRICTED)"
                } else {
                    "📍 LOCATION: ${location.latitude}, ${location.longitude}\n✅ ZONE: Safe Area"
                }
            } else {
                "📍 LOCATION: Unable to get current location"
            }
            
            statusText.text = "$baseStatus\n\n$locationStatus"
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = "${requireContext().packageName}/${GeoCamAccessibilityService::class.java.canonicalName}"
        val enabledServicesSetting = Settings.Secure.getString(
            requireContext().contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        
        return enabledServicesSetting.contains(expectedComponentName)
    }

    private fun isLocationServiceEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(LocationManager::class.java)
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }    override fun onDestroyView() {
        super.onDestroyView()
        updateRunnable?.let { updateHandler.removeCallbacks(it) }
    }
}
