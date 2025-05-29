package com.example.geocamoff.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.geocamoff.data.Geofence
import com.example.geocamoff.databinding.ItemGeofenceBinding

class GeofenceAdapter(
    private val geofences: MutableList<Geofence>,
    private val onDelete: (Geofence) -> Unit
) : RecyclerView.Adapter<GeofenceAdapter.GeofenceViewHolder>() {

    inner class GeofenceViewHolder(val binding: ItemGeofenceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GeofenceViewHolder {
        val binding = ItemGeofenceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GeofenceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GeofenceViewHolder, position: Int) {
        val geofence = geofences[position]
        holder.binding.textViewItemName.text = geofence.name
        holder.binding.textViewItemCoords.text = "Lat: %.5f, Lon: %.5f, R: %.0fm".format(geofence.latitude, geofence.longitude, geofence.radiusMetres)
        holder.binding.textViewItemMessage.text = geofence.alertMessage
        holder.binding.buttonDeleteItem.setOnClickListener {
            onDelete(geofence)
        }
    }

    override fun getItemCount(): Int = geofences.size
}