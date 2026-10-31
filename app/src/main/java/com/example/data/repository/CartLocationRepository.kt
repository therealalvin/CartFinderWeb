package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.DistanceUnit
import com.example.data.model.GolfCartLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CartLocationRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("golf_cart_finder_prefs", Context.MODE_PRIVATE)

    private val _currentParkedLocation = MutableStateFlow<GolfCartLocation?>(null)
    val currentParkedLocation: StateFlow<GolfCartLocation?> = _currentParkedLocation.asStateFlow()

    private val _preferredUnit = MutableStateFlow(DistanceUnit.YARDS)
    val preferredUnit: StateFlow<DistanceUnit> = _preferredUnit.asStateFlow()

    init {
        // Clear any legacy history to respect user privacy preference
        if (prefs.contains("history_json")) {
            prefs.edit().remove("history_json").apply()
        }
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        val hasActive = prefs.getBoolean(KEY_HAS_ACTIVE, false)
        if (hasActive) {
            val id = prefs.getString(KEY_ID, "") ?: ""
            val lat = prefs.getString(KEY_LAT, "0.0")?.toDoubleOrNull() ?: 0.0
            val lng = prefs.getString(KEY_LNG, "0.0")?.toDoubleOrNull() ?: 0.0
            val acc = prefs.getFloat(KEY_ACC, 0f)
            val time = prefs.getLong(KEY_TIME, 0L)
            val label = prefs.getString(KEY_LABEL, "") ?: ""
            val note = prefs.getString(KEY_NOTE, "") ?: ""
            if (lat != 0.0 && lng != 0.0) {
                _currentParkedLocation.value = GolfCartLocation(
                    id = id,
                    latitude = lat,
                    longitude = lng,
                    accuracyMeters = acc,
                    timestamp = time,
                    spotLabel = label,
                    note = note
                )
            }
        } else {
            _currentParkedLocation.value = null
        }

        val unitName = prefs.getString(KEY_UNIT, DistanceUnit.YARDS.name) ?: DistanceUnit.YARDS.name
        _preferredUnit.value = try {
            DistanceUnit.valueOf(unitName)
        } catch (e: Exception) {
            DistanceUnit.YARDS
        }
    }

    fun saveParkedLocation(location: GolfCartLocation) {
        _currentParkedLocation.value = location
        prefs.edit()
            .putBoolean(KEY_HAS_ACTIVE, true)
            .putString(KEY_ID, location.id)
            .putString(KEY_LAT, location.latitude.toString())
            .putString(KEY_LNG, location.longitude.toString())
            .putFloat(KEY_ACC, location.accuracyMeters)
            .putLong(KEY_TIME, location.timestamp)
            .putString(KEY_LABEL, location.spotLabel)
            .putString(KEY_NOTE, location.note)
            .apply()
    }

    fun clearParkedLocation() {
        _currentParkedLocation.value = null
        prefs.edit()
            .putBoolean(KEY_HAS_ACTIVE, false)
            .remove(KEY_ID)
            .remove(KEY_LAT)
            .remove(KEY_LNG)
            .remove(KEY_ACC)
            .remove(KEY_TIME)
            .remove(KEY_LABEL)
            .remove(KEY_NOTE)
            .apply()
    }

    fun setPreferredUnit(unit: DistanceUnit) {
        _preferredUnit.value = unit
        prefs.edit().putString(KEY_UNIT, unit.name).apply()
    }

    companion object {
        private const val KEY_HAS_ACTIVE = "has_active_location"
        private const val KEY_ID = "active_id"
        private const val KEY_LAT = "active_lat"
        private const val KEY_LNG = "active_lng"
        private const val KEY_ACC = "active_acc"
        private const val KEY_TIME = "active_time"
        private const val KEY_LABEL = "active_label"
        private const val KEY_NOTE = "active_note"
        private const val KEY_UNIT = "pref_distance_unit"
    }
}
