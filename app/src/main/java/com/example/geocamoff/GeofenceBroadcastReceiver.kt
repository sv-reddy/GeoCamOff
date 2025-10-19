package com.example.geocamoff

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e("GeofenceReceiver", "GeofencingEvent is null")
            return
        }
        if (geofencingEvent.hasError()) {
            val errorMessage = when (geofencingEvent.errorCode) {
                GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> "Geofence service is not available"
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> "Too many geofences"
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> "Too many pending intents"
                else -> "Unknown geofence error"
            }
            Log.e("GeofenceReceiver", "Error: $errorMessage (code: ${geofencingEvent.errorCode})")
            return
        }
        val transition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences
        
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // Entered a restricted zone
            StateManager.updateGeofenceState(context, true)
        } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            // Exited a restricted zone
            StateManager.updateGeofenceState(context, false)
        }
    }
}
