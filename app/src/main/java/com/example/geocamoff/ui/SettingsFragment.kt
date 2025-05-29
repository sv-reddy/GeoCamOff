package com.example.geocamoff.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.geocamoff.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = requireContext().getSharedPreferences("GeoCamOffPrefs", 0)

        // Initialize switches
        binding.switchAlerts.isChecked = prefs.getBoolean("alerts_enabled", true)
        val isDarkMode = prefs.getBoolean("dark_mode_enabled", false)
        binding.switchDarkMode.isChecked = isDarkMode

        // Handle alert toggle
        binding.switchAlerts.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("alerts_enabled", isChecked).apply()
        }

        // Handle dark mode toggle with proper theme switching
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode_enabled", isChecked).apply()
            val mode = if (isChecked) {
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            } else {
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            }
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode)
            // Force activity recreation to apply theme
            requireActivity().recreate()
        }

        // Handle geofence settings button
        binding.buttonOpenGeofenceSettings.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), com.example.geocamoff.SettingsActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
