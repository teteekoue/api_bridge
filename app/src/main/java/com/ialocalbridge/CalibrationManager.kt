package com.ialocalbridge

import android.content.Context
import com.google.gson.Gson
import com.ialocalbridge.models.ProviderCoordinates

class CalibrationManager(context: Context) {
    private val prefs = context.getSharedPreferences("calibration_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveCoordinates(providerName: String, coords: ProviderCoordinates) {
        prefs.edit().putString(providerName, gson.toJson(coords)).apply()
    }

    fun getCoordinates(providerName: String): ProviderCoordinates {
        val json = prefs.getString(providerName, null)
        return if (json != null) gson.fromJson(json, ProviderCoordinates::class.java)
        else ProviderCoordinates()
    }

    fun getAllProviderNames(): List<String> {
        return prefs.all.keys.toList().filter { it != "last_used_provider" }
    }

    fun deleteProvider(name: String) {
        prefs.edit().remove(name).apply()
    }
}
