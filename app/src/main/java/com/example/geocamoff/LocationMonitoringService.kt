package com.example.geocamoff

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat

class LocationMonitoringService : Service(), LocationListener {
      companion object {
        private const val TAG = "LocationMonitoring"
        private const val LOCATION_UPDATE_INTERVAL = 500L // 0.5 seconds (reduced from 1 second)
        private const val LOCATION_UPDATE_DISTANCE = 0.5f // 0.5 meters (reduced from 1 meter)
    }private var locationManager: LocationManager? = null
    private var lastKnownLocation: Location? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LocationMonitoringService created")
        
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        startLocationUpdates()
        
        // Also monitor location service state changes
        monitorLocationServiceState()
    }

    private fun startLocationUpdates() {
        if (!hasLocationPermissions()) {
            Log.w(TAG, "Location permissions not granted")
            return
        }

        try {
            // Request location updates from both GPS and Network providers
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LOCATION_UPDATE_INTERVAL,
                    LOCATION_UPDATE_DISTANCE,
                    this
                )
                Log.d(TAG, "GPS location updates requested")
            }

            if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    LOCATION_UPDATE_INTERVAL,
                    LOCATION_UPDATE_DISTANCE,
                    this
                )
                Log.d(TAG, "Network location updates requested")
            }

            // Get last known location immediately
            getLastKnownLocation()
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception requesting location updates: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location updates: ${e.message}")
        }
    }

    private fun getLastKnownLocation() {
        if (!hasLocationPermissions()) return

        try {
            val gpsLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLocation = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            // Use the more recent location
            val bestLocation = when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.time > networkLocation.time) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> null
            }

            bestLocation?.let { location ->
                onLocationChanged(location)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting last known location: ${e.message}")
        }
    }    private fun monitorLocationServiceState() {
        // Check location service state periodically
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                checkLocationServiceState()
                handler.postDelayed(this, 500) // Check every 0.5 seconds (reduced from 1 second)
            }
        }
        handler.post(runnable)
    }

    private fun checkLocationServiceState() {
        val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        val isNetworkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false
        val isLocationEnabled = isGpsEnabled || isNetworkEnabled
        
        // Update StateManager with location service state
        StateManager.updateLocationState(this, isLocationEnabled)
    }

    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    override fun onLocationChanged(location: Location) {
        Log.d(TAG, "Location changed: ${location.latitude}, ${location.longitude}")
          lastKnownLocation = location
        
        // Check if current location is in any restricted area
        val restrictedAreas = RestrictedAreaLoader.loadRestrictedAreas(this)
        var inRestrictedZone = false
        
        for (area in restrictedAreas) {
            if (isLocationInPolygon(location, area)) {
                inRestrictedZone = true
                Log.d(TAG, "Location is in restricted area: ${area.name}")
                break
            }
        }
        
        // Update StateManager
        StateManager.updateGeofenceState(this, inRestrictedZone)
    }    private fun isLocationInPolygon(location: Location, polygonArea: PolygonGeofenceData): Boolean {
        // Use the existing PolygonGeofenceUtils to check if location is inside polygon
        val currentPoint = LatLngPoint(location.latitude, location.longitude)
        return PolygonGeofenceUtils.isInsidePolygonGeofence(currentPoint, polygonArea)
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {
        Log.d(TAG, "Location provider status changed: $provider, status: $status")
        checkLocationServiceState()
    }

    override fun onProviderEnabled(provider: String) {
        Log.d(TAG, "Location provider enabled: $provider")
        checkLocationServiceState()
        
        // Restart location updates when provider is enabled
        if (hasLocationPermissions()) {
            try {
                locationManager?.requestLocationUpdates(
                    provider,
                    LOCATION_UPDATE_INTERVAL,
                    LOCATION_UPDATE_DISTANCE,
                    this
                )
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception restarting location updates: ${e.message}")
            }
        }
    }

    override fun onProviderDisabled(provider: String) {
        Log.d(TAG, "Location provider disabled: $provider")
        checkLocationServiceState()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LocationMonitoringService destroyed")
        
        try {
            locationManager?.removeUpdates(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing location updates: ${e.message}")
        }
    }

    fun getCurrentLocation(): Location? = lastKnownLocation
}
