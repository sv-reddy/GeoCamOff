package com.example.geocamoff

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PolygonGeofenceAdapter : RecyclerView.Adapter<PolygonGeofenceAdapter.PolygonGeofenceViewHolder>() {
    private val geofences = mutableListOf<PolygonGeofenceData>()

    class PolygonGeofenceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.text_name)
        val centerText: TextView = view.findViewById(R.id.text_center)
        val areaText: TextView = view.findViewById(R.id.text_area)
        val pointsText: TextView = view.findViewById(R.id.text_points)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PolygonGeofenceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_geofence, parent, false)
        return PolygonGeofenceViewHolder(view)
    }    override fun onBindViewHolder(holder: PolygonGeofenceViewHolder, position: Int) {
        val geofence = geofences[position]
        val center = geofence.getCenterPoint()
        val area = PolygonGeofenceUtils.calculatePolygonArea(geofence.coordinates)
        
        holder.nameText.text = geofence.name
        holder.centerText.text = holder.itemView.context.getString(
            R.string.polygon_center_format, 
            center.latitude, 
            center.longitude
        )
        holder.areaText.text = holder.itemView.context.getString(
            R.string.polygon_area_format, 
            area / 1_000_000
        )
        holder.pointsText.text = holder.itemView.context.getString(
            R.string.polygon_points_format, 
            geofence.coordinates.size
        )
    }

    override fun getItemCount() = geofences.size

    fun addGeofence(geofence: PolygonGeofenceData) {
        geofences.add(geofence)
        notifyItemInserted(geofences.size - 1)
    }

    fun clearGeofences() {
        val size = geofences.size
        geofences.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun getGeofences(): List<PolygonGeofenceData> = geofences.toList()
}
