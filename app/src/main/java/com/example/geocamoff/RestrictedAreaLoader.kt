package com.example.geocamoff

import android.content.Context
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

object RestrictedAreaLoader {
    
    fun loadRestrictedAreas(context: Context): List<PolygonGeofenceData> {
        val geofences = mutableListOf<PolygonGeofenceData>()
        
        try {
            val inputStream = context.assets.open("restricted_areas.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonText = reader.use { it.readText() }
            
            val jsonArray = JSONArray(jsonText)
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                try {
                    val name = jsonObject.getString("name")
                    val id = jsonObject.getString("id")
                    val coordinatesArray = jsonObject.getJSONArray("coordinates")
                    
                    val coordinates = mutableListOf<LatLngPoint>()
                    for (j in 0 until coordinatesArray.length()) {
                        val coordObject = coordinatesArray.getJSONObject(j)
                        val lat = coordObject.getDouble("latitude")
                        val lng = coordObject.getDouble("longitude")
                        coordinates.add(LatLngPoint(lat, lng))
                    }
                    
                    // Validate polygon before adding
                    val validationError = PolygonGeofenceUtils.validatePolygon(coordinates)
                    if (validationError == null) {
                        val geofence = PolygonGeofenceData(name, coordinates, id)
                        geofences.add(geofence)
                    } else {
                        // Log the validation error but continue processing other entries
                        android.util.Log.w("RestrictedAreaLoader", 
                            "Skipping invalid polygon '$name': $validationError")
                    }
                } catch (e: Exception) {
                    // Skip malformed geofence entries
                    android.util.Log.w("RestrictedAreaLoader", 
                        "Skipping malformed geofence entry: ${e.message}")
                    continue
                }
            }
        } catch (e: Exception) {
            // Log error but return partial list if any entries were successfully parsed
            android.util.Log.e("RestrictedAreaLoader", 
                "Error reading restricted areas file: ${e.message}")        }
        
        return geofences
    }
}
