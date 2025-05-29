package com.example.geocamoff.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.geocamoff.databinding.FragmentStatusBinding
import com.example.geocamoff.repository.RestrictedLocationsRepository

class StatusFragment : Fragment() {
    private var _binding: FragmentStatusBinding? = null
    private val binding get() = _binding!!
    private lateinit var cameraManager: CameraManager

    private val cameraStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.example.geocamoff.CAMERA_STATE" -> {
                    val isInUse = intent.getBooleanExtra("camera_in_use", false)
                    updateCameraStatus(isInUse)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Register for camera state updates
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            cameraStateReceiver,
            IntentFilter("com.example.geocamoff.CAMERA_STATE")
        )

        // Display restricted locations in the status TextView
        val locationsText = RestrictedLocationsRepository.restrictedLocations.joinToString("\n") {
            "${it.name} (Lat: ${it.latitude}, Lng: ${it.longitude}, Radius: ${it.radiusMeters}m)"
        }
        binding.textViewGeoStatus.text = "Restricted Locations:\n$locationsText"

        binding.buttonCheckNow.setOnClickListener {
            checkCameraStatus()
        }

        // Initial camera status check
        checkCameraStatus()
    }

    private fun checkCameraStatus() {
        try {
            val isAnyInUse = cameraManager.cameraIdList.any { cameraId ->
                try {
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    val facing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                    facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK ||
                            facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT
                } catch (e: Exception) {
                    false
                }
            }
            updateCameraStatus(isAnyInUse)
        } catch (e: Exception) {
            updateCameraStatus(false)
        }
    }

    private fun updateCameraStatus(isInUse: Boolean) {
        val statusText = if (isInUse) {
            "Camera status: IN USE"
        } else {
            "Camera status: Not in use"
        }
        binding.textViewCameraStatus.text = statusText
    }

    override fun onDestroyView() {
        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(cameraStateReceiver)
        super.onDestroyView()
        _binding = null
    }
}
