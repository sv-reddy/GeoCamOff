package com.example.geocamoff

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

/**
 * Simple file-based configuration manager for security settings
 * Stores the security phone number in a properties file in the app's internal storage
 */
object SecurityConfigManager {
    private const val TAG = "SecurityConfig"
    private const val CONFIG_FILE_NAME = "security_config.properties"
    private const val KEY_SECURITY_PHONE = "security_phone"
    private const val KEY_SECURITY_EMAIL = "security_email"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    
    // Default values
    private const val DEFAULT_SECURITY_PHONE = "7013230152"
    private const val DEFAULT_SECURITY_EMAIL = "babelgautam16@gmail.com"
    private const val DEFAULT_NOTIFICATIONS_ENABLED = "true"
    
    /**
     * Get the security phone number from the config file
     */
    fun getSecurityPhone(context: Context): String {
        return getConfigValue(context, KEY_SECURITY_PHONE, DEFAULT_SECURITY_PHONE)
    }
    
    /**
     * Get the security email from the config file
     */
    fun getSecurityEmail(context: Context): String {
        return getConfigValue(context, KEY_SECURITY_EMAIL, DEFAULT_SECURITY_EMAIL)
    }
    
    /**
     * Check if security notifications are enabled
     */
    fun isNotificationsEnabled(context: Context): Boolean {
        val value = getConfigValue(context, KEY_NOTIFICATIONS_ENABLED, DEFAULT_NOTIFICATIONS_ENABLED)
        return value.lowercase() == "true"
    }
    
    /**
     * Set the security phone number in the config file
     */
    fun setSecurityPhone(context: Context, phone: String) {
        setConfigValue(context, KEY_SECURITY_PHONE, phone)
    }
    
    /**
     * Set the security email in the config file
     */
    fun setSecurityEmail(context: Context, email: String) {
        setConfigValue(context, KEY_SECURITY_EMAIL, email)
    }
    
    /**
     * Set notifications enabled status
     */
    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        setConfigValue(context, KEY_NOTIFICATIONS_ENABLED, enabled.toString())
    }
    
    /**
     * Get all security settings
     */
    fun getSecuritySettings(context: Context): SecuritySettings {
        return SecuritySettings(
            phone = getSecurityPhone(context),
            email = getSecurityEmail(context),
            enabled = isNotificationsEnabled(context)
        )
    }
    
    /**
     * Update security settings
     */
    fun updateSecuritySettings(context: Context, phone: String?, email: String?, enabled: Boolean) {
        phone?.let { setSecurityPhone(context, it) }
        email?.let { setSecurityEmail(context, it) }
        setNotificationsEnabled(context, enabled)
    }
    
    private fun getConfigValue(context: Context, key: String, defaultValue: String): String {
        return try {
            val configFile = File(context.filesDir, CONFIG_FILE_NAME)
            if (!configFile.exists()) {
                Log.d(TAG, "Config file doesn't exist, creating with default values")
                createDefaultConfigFile(context)
            }
            
            val properties = Properties()
            FileInputStream(configFile).use { inputStream ->
                properties.load(inputStream)
            }
            
            val value = properties.getProperty(key, defaultValue)
            Log.d(TAG, "Retrieved config value: $key = $value")
            value
            
        } catch (e: Exception) {
            Log.e(TAG, "Error reading config file for key $key: ${e.message}", e)
            defaultValue
        }
    }
    
    private fun setConfigValue(context: Context, key: String, value: String) {
        try {
            val configFile = File(context.filesDir, CONFIG_FILE_NAME)
            val properties = Properties()
            
            // Load existing properties if file exists
            if (configFile.exists()) {
                FileInputStream(configFile).use { inputStream ->
                    properties.load(inputStream)
                }
            }
            
            // Set the new value
            properties.setProperty(key, value)
            
            // Save back to file
            FileOutputStream(configFile).use { outputStream ->
                properties.store(outputStream, "Security Configuration")
            }
            
            Log.d(TAG, "Set config value: $key = $value")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error writing config file for key $key: ${e.message}", e)
        }
    }
    
    private fun createDefaultConfigFile(context: Context) {
        try {
            val configFile = File(context.filesDir, CONFIG_FILE_NAME)
            val properties = Properties()
            
            properties.setProperty(KEY_SECURITY_PHONE, DEFAULT_SECURITY_PHONE)
            properties.setProperty(KEY_SECURITY_EMAIL, DEFAULT_SECURITY_EMAIL)
            properties.setProperty(KEY_NOTIFICATIONS_ENABLED, DEFAULT_NOTIFICATIONS_ENABLED)
            
            FileOutputStream(configFile).use { outputStream ->
                properties.store(outputStream, "Default Security Configuration")
            }
            
            Log.d(TAG, "Created default config file")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating default config file: ${e.message}", e)
        }
    }
    
    /**
     * Get the config file path (for debugging)
     */
    fun getConfigFilePath(context: Context): String {
        return File(context.filesDir, CONFIG_FILE_NAME).absolutePath
    }
    
    /**
     * Check if config file exists
     */
    fun configFileExists(context: Context): Boolean {
        return File(context.filesDir, CONFIG_FILE_NAME).exists()
    }
    
    data class SecuritySettings(
        val phone: String,
        val email: String,
        val enabled: Boolean
    )
}
