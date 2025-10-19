package com.example.geocamoff

data class LatLngPoint(
    val latitude: Double,
    val longitude: Double
)

data class PolygonGeofenceData(
    val name: String,
    val coordinates: List<LatLngPoint>,
    val id: String = System.currentTimeMillis().toString()
) {
    init {
        require(coordinates.size >= 3) { "Polygon must have at least 3 points" }
        require(coordinates.size <= 100) { "Polygon cannot have more than 100 points" }
    }
    
    // Helper function to get the center point of the polygon for display purposes
    fun getCenterPoint(): LatLngPoint {
        val avgLat = coordinates.map { it.latitude }.average()
        val avgLng = coordinates.map { it.longitude }.average()
        return LatLngPoint(avgLat, avgLng)
    }
    
    // Helper function to get the bounding box for quick elimination
    fun getBoundingBox(): BoundingBox {
        val minLat = coordinates.minOf { it.latitude }
        val maxLat = coordinates.maxOf { it.latitude }
        val minLng = coordinates.minOf { it.longitude }
        val maxLng = coordinates.maxOf { it.longitude }
        return BoundingBox(minLat, maxLat, minLng, maxLng)
    }
}

data class BoundingBox(
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double
)
