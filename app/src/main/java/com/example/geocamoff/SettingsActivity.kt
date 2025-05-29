package com.example.geocamoff

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.geocamoff.data.Geofence
import com.example.geocamoff.databinding.ActivitySettingsBinding
import com.example.geocamoff.services.OverlayService
import com.example.geocamoff.ui.GeofenceAdapter
import com.example.geocamoff.utils.Constants
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPreferences: android.content.SharedPreferences
    private val gson = Gson()
    private val geofenceList = mutableListOf<Geofence>()
    private lateinit var adapter: GeofenceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        adapter = GeofenceAdapter(geofenceList) { geofence ->
            geofenceList.remove(geofence)
            saveGeofences()
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "Geofence deleted", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerViewGeofences.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewGeofences.adapter = adapter

        loadGeofences()

        binding.buttonAddGeofence.setOnClickListener {
            val name = binding.editTextGeofenceName.text.toString().trim()
            val latStr = binding.editTextLatitude.text.toString().trim()
            val lonStr = binding.editTextLongitude.text.toString().trim()
            val radiusStr = binding.editTextRadius.text.toString().trim()
            val alertMsg = binding.editTextAlertMessage.text.toString().trim()
            if (name.isEmpty() || latStr.isEmpty() || lonStr.isEmpty() || radiusStr.isEmpty() || alertMsg.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val latitude = latStr.toDoubleOrNull()
            val longitude = lonStr.toDoubleOrNull()
            val radius = radiusStr.toFloatOrNull()
            if (latitude == null || longitude == null || radius == null) {
                Toast.makeText(this, "Invalid coordinates or radius", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val geofence = Geofence(
                id = UUID.randomUUID().toString(),
                name = name,
                latitude = latitude,
                longitude = longitude,
                radiusMetres = radius,
                alertMessage = alertMsg
            )
            geofenceList.add(geofence)
            saveGeofences()
            adapter.notifyDataSetChanged()
            binding.editTextGeofenceName.text?.clear()
            binding.editTextLatitude.text?.clear()
            binding.editTextLongitude.text?.clear()
            binding.editTextRadius.text?.clear()
            binding.editTextAlertMessage.text?.clear()
            Toast.makeText(this, "Geofence added", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveGeofences() {
        val jsonString = gson.toJson(geofenceList)
        sharedPreferences.edit().putString(Constants.PREF_KEY_GEOFENCES, jsonString).apply()
        // Notify service to update
        val intent = Intent(this, OverlayService::class.java).apply {
            action = Constants.ACTION_UPDATE_GEOFENCES
        }
        startService(intent)
    }

    private fun loadGeofences() {
        val jsonString = sharedPreferences.getString(Constants.PREF_KEY_GEOFENCES, null)
        if (jsonString != null) {
            val type = object : TypeToken<MutableList<Geofence>>() {}.type
            geofenceList.clear()
            geofenceList.addAll(gson.fromJson(jsonString, type))
            adapter.notifyDataSetChanged()
        }
    }
}