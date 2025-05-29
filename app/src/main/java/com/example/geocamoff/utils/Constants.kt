package com.example.geocamoff.utils

object Constants {
    const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
    const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
    const val ACTION_UPDATE_GEOFENCES = "ACTION_UPDATE_GEOFENCES" // For service to update its list

    const val NOTIFICATION_CHANNEL_ID = "GeoCamOffChannel"
    const val NOTIFICATION_ID = 101

    const val PREFS_NAME = "GeoCamOffPrefs"
    const val PREF_KEY_GEOFENCES = "geofences_list"

    const val REQUEST_CODE_OVERLAY_PERMISSION = 123
    const val REQUEST_CODE_LOCATION_PERMISSIONS = 124
}