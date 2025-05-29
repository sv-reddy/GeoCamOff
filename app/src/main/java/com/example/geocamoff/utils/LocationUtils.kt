package com.example.geocamoff.utils

import android.location.Location
import com.example.geocamoff.data.Geofence

object LocationUtils {    fun isInsideGeofence(currentLocation: Location, geofence: Geofence): Boolean {
        val distanceMeters = calculateDistance(
            currentLocation.latitude, currentLocation.longitude,
            geofence.latitude, geofence.longitude
        )
        android.util.Log.d("LocationUtils", "Distance to ${geofence.name}: $distanceMeters meters (radius: ${geofence.radiusMetres}m)")
        return distanceMeters <= geofence.radiusMetres
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
}