package com.example.geocamoff.model

data class RestrictedLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float
)
