package com.example.geocamoff

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SettingsFragment : Fragment() {
    private lateinit var adapter: PolygonGeofenceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Setup RecyclerView and Adapter
        val recyclerView = view.findViewById<RecyclerView>(R.id.geofence_list)
        adapter = PolygonGeofenceAdapter()
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // Load predefined restricted areas from static file
        loadRestrictedAreas()

        return view
    }

    private fun loadRestrictedAreas() {
        val restrictedAreas = RestrictedAreaLoader.loadRestrictedAreas(requireContext())
        
        for (polygonGeofence in restrictedAreas) {
            adapter.addGeofence(polygonGeofence)
        }
    }
}
