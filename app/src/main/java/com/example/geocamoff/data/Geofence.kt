package com.example.geocamoff.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize // Make sure to add kotlin-parcelize plugin if not already

@Parcelize // For passing in Intents easily
data class Geofence(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMetres: Float,
    val alertMessage: String
) : Parcelable