package com.example.geocamoff

import kotlin.math.*

object PolygonGeofenceUtils {
    
    /**
     * Determines if a point is inside a polygon using the ray casting algorithm
     * This is more accurate than circular geofences for irregular shaped areas
     */
    fun isPointInPolygon(point: LatLngPoint, polygon: List<LatLngPoint>): Boolean {
        if (polygon.size < 3) return false
        
        val x = point.longitude
        val y = point.latitude
        var inside = false
        
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val xi = polygon[i].longitude
            val yi = polygon[i].latitude
            val xj = polygon[j].longitude
            val yj = polygon[j].latitude
            
            if (((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi)) {
                inside = !inside
            }
            j = i
        }
        
        return inside
    }
    
    /**
     * Quick bounding box check before expensive polygon calculation
     * This optimizes performance by eliminating obviously outside points
     */
    fun isPointInBoundingBox(point: LatLngPoint, boundingBox: BoundingBox): Boolean {
        return point.latitude >= boundingBox.minLatitude &&
               point.latitude <= boundingBox.maxLatitude &&
               point.longitude >= boundingBox.minLongitude &&
               point.longitude <= boundingBox.maxLongitude
    }
    
    /**
     * Checks if a point is inside a polygon geofence
     * Uses bounding box optimization followed by precise polygon detection
     */
    fun isInsidePolygonGeofence(point: LatLngPoint, geofence: PolygonGeofenceData): Boolean {
        // Quick bounding box check first
        if (!isPointInBoundingBox(point, geofence.getBoundingBox())) {
            return false
        }
        
        // If within bounding box, do precise polygon check
        return isPointInPolygon(point, geofence.coordinates)
    }
    
    /**
     * Calculate the approximate area of a polygon in square meters
     * Useful for validation and display purposes
     */
    fun calculatePolygonArea(coordinates: List<LatLngPoint>): Double {
        if (coordinates.size < 3) return 0.0
        
        var area = 0.0
        val n = coordinates.size
        
        for (i in coordinates.indices) {
            val j = (i + 1) % n
            val lat1 = Math.toRadians(coordinates[i].latitude)
            val lat2 = Math.toRadians(coordinates[j].latitude)
            val lng1 = Math.toRadians(coordinates[i].longitude)
            val lng2 = Math.toRadians(coordinates[j].longitude)
            
            area += (lng2 - lng1) * (2 + sin(lat1) + sin(lat2))
        }
        
        area = abs(area) * 6378137.0 * 6378137.0 / 2.0 // Earth radius squared
        return area
    }
    
    /**
     * Validate that a polygon is properly formed
     */
    fun validatePolygon(coordinates: List<LatLngPoint>): String? {
        if (coordinates.size < 3) {
            return "Polygon must have at least 3 points"
        }
        
        if (coordinates.size > 100) {
            return "Polygon cannot have more than 100 points"
        }
        
        // Check for valid latitude/longitude ranges
        for (point in coordinates) {
            if (point.latitude < -90 || point.latitude > 90) {
                return "Invalid latitude: ${point.latitude}"
            }
            if (point.longitude < -180 || point.longitude > 180) {
                return "Invalid longitude: ${point.longitude}"
            }
        }
        
        // Check that polygon has some area (not all points collinear)
        val area = calculatePolygonArea(coordinates)
        if (area < 1.0) { // Less than 1 square meter
            return "Polygon area too small or points are collinear"
        }
        
        return null // Valid polygon
    }
}
