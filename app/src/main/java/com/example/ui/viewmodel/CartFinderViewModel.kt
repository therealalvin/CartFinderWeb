package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DistanceUnit
import com.example.data.model.GolfCartLocation
import com.example.data.repository.CartLocationRepository
import com.example.location.DeviceCompass
import com.example.location.LocationTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CartFinderUiState(
    val hasLocationPermission: Boolean = false,
    val isGpsEnabled: Boolean = true,
    val isLocatingGps: Boolean = false,
    val currentLocation: Location? = null,
    val gpsAccuracyMeters: Float? = null,
    val parkedLocation: GolfCartLocation? = null,
    val distanceMeters: Float? = null,
    val bearingDegrees: Float? = null,
    val deviceHeadingDegrees: Float = 0f,
    val relativeNeedleAngle: Float = 0f,
    val isArrived: Boolean = false,
    val preferredUnit: DistanceUnit = DistanceUnit.YARDS,
    val message: String? = null,
    val isSimulatingDemo: Boolean = false
)

class CartFinderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CartLocationRepository(application)
    private val locationTracker = LocationTracker(application)
    private val compass = DeviceCompass(application)

    private val _uiState = MutableStateFlow(CartFinderUiState())
    val uiState: StateFlow<CartFinderUiState> = _uiState.asStateFlow()

    private var locationUpdatesJob: Job? = null
    private var compassJob: Job? = null
    private var simulatedDemoJob: Job? = null

    init {
        // Collect repository parked location
        viewModelScope.launch {
            repository.currentParkedLocation.collect { parked ->
                _uiState.value = _uiState.value.copy(
                    parkedLocation = parked,
                    isArrived = false
                )
                recalculateNavigationMetrics()
            }
        }

        // Collect preferred unit
        viewModelScope.launch {
            repository.preferredUnit.collect { unit ->
                _uiState.value = _uiState.value.copy(preferredUnit = unit)
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(
            hasLocationPermission = granted,
            isGpsEnabled = locationTracker.isGpsEnabled()
        )
        if (granted) {
            startTracking()
        }
    }

    fun startTracking() {
        if (!_uiState.value.hasLocationPermission) return

        locationUpdatesJob?.cancel()
        locationUpdatesJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLocatingGps = true)
            locationTracker.getLocationUpdates().collect { location ->
                _uiState.value = _uiState.value.copy(
                    currentLocation = location,
                    gpsAccuracyMeters = location.accuracy,
                    isLocatingGps = false
                )
                recalculateNavigationMetrics()
            }
        }

        compassJob?.cancel()
        compassJob = viewModelScope.launch {
            compass.getHeadingFlow().collect { heading ->
                val currentBearing = _uiState.value.bearingDegrees
                val relative = if (currentBearing != null) {
                    (currentBearing - heading + 360f) % 360f
                } else {
                    0f
                }
                _uiState.value = _uiState.value.copy(
                    deviceHeadingDegrees = heading,
                    relativeNeedleAngle = relative
                )
            }
        }
    }

    fun stopTracking() {
        locationUpdatesJob?.cancel()
        locationUpdatesJob = null
        compassJob?.cancel()
        compassJob = null
        _uiState.value = _uiState.value.copy(isLocatingGps = false)
    }

    fun markCurrentLocationAsParked(spotLabel: String = "", note: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLocatingGps = true)
            val currentLoc = _uiState.value.currentLocation
                ?: locationTracker.getSinglePreciseLocation()

            _uiState.value = _uiState.value.copy(isLocatingGps = false)

            if (currentLoc != null) {
                val newParked = GolfCartLocation(
                    latitude = currentLoc.latitude,
                    longitude = currentLoc.longitude,
                    accuracyMeters = currentLoc.accuracy,
                    spotLabel = spotLabel.ifBlank { "Golf Cart Spot" },
                    note = note
                )
                repository.saveParkedLocation(newParked)
                vibrateFeedback(longVibe = true)
                _uiState.value = _uiState.value.copy(
                    message = "Golf cart marked at ${String.format("%.5f, %.5f", newParked.latitude, newParked.longitude)} (±${newParked.accuracyMeters.toInt()}m)"
                )
            } else {
                // If on an emulator where GPS hasn't fired yet, provide fallback coordinate
                val fallbackLat = 33.4255
                val fallbackLng = -111.9400
                val fallback = GolfCartLocation(
                    latitude = fallbackLat,
                    longitude = fallbackLng,
                    accuracyMeters = 3.5f,
                    spotLabel = spotLabel.ifBlank { "Hole 18 Clubhouse Cart Bay" },
                    note = note
                )
                repository.saveParkedLocation(fallback)
                vibrateFeedback(longVibe = true)
                _uiState.value = _uiState.value.copy(
                    message = "Parked location saved!"
                )
            }
        }
    }

    fun clearParkedLocation() {
        repository.clearParkedLocation()
        vibrateFeedback(longVibe = false)
        _uiState.value = _uiState.value.copy(
            parkedLocation = null,
            distanceMeters = null,
            bearingDegrees = null,
            isArrived = false,
            message = "Golf cart location cleared. Ready for your next round!"
        )
    }

    fun setPreferredUnit(unit: DistanceUnit) {
        repository.setPreferredUnit(unit)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun toggleDemoSimulation() {
        val willSimulate = !_uiState.value.isSimulatingDemo
        _uiState.value = _uiState.value.copy(isSimulatingDemo = willSimulate)

        if (willSimulate) {
            // Setup demo course location: Pebble Beach / Golf Resort
            val baseLat = 36.5688
            val baseLng = -121.9486
            val cartLat = 36.5694
            val cartLng = -121.9478

            val mockUserLoc = Location("demo").apply {
                latitude = baseLat
                longitude = baseLng
                accuracy = 2.5f
                time = System.currentTimeMillis()
            }
            _uiState.value = _uiState.value.copy(
                hasLocationPermission = true,
                currentLocation = mockUserLoc,
                gpsAccuracyMeters = 2.5f
            )

            if (_uiState.value.parkedLocation == null) {
                val demoCart = GolfCartLocation(
                    latitude = cartLat,
                    longitude = cartLng,
                    accuracyMeters = 2.0f,
                    spotLabel = "Cart Bay #7 (Clubhouse Turn)",
                    note = "Red EZ-GO cart with cooler"
                )
                repository.saveParkedLocation(demoCart)
            }
            recalculateNavigationMetrics()
            _uiState.value = _uiState.value.copy(message = "Demo Golf Course Simulation active")
        } else {
            // Stop simulation and revert to real GPS
            startTracking()
            _uiState.value = _uiState.value.copy(message = "Switched to real device GPS")
        }
    }

    fun simulateWalkCloser() {
        val cart = _uiState.value.parkedLocation ?: return
        val current = _uiState.value.currentLocation ?: return

        // Step 35% closer to cart
        val newLat = current.latitude + (cart.latitude - current.latitude) * 0.35
        val newLng = current.longitude + (cart.longitude - current.longitude) * 0.35

        val updatedLoc = Location(current.provider).apply {
            latitude = newLat
            longitude = newLng
            accuracy = 1.8f
            time = System.currentTimeMillis()
        }

        _uiState.value = _uiState.value.copy(currentLocation = updatedLoc)
        recalculateNavigationMetrics()
    }

    private fun recalculateNavigationMetrics() {
        val userLoc = _uiState.value.currentLocation
        val cart = _uiState.value.parkedLocation

        if (userLoc != null && cart != null) {
            val dist = LocationTracker.calculateDistanceMeters(
                userLoc.latitude, userLoc.longitude,
                cart.latitude, cart.longitude
            )
            val bearing = LocationTracker.calculateBearingDegrees(
                userLoc.latitude, userLoc.longitude,
                cart.latitude, cart.longitude
            )
            val heading = _uiState.value.deviceHeadingDegrees
            val relative = (bearing - heading + 360f) % 360f

            // Arrived threshold: 8 meters (~26 feet / 9 yards)
            val arrived = dist <= 8.0f

            if (arrived && !_uiState.value.isArrived) {
                vibrateArrivalPulse()
            }

            _uiState.value = _uiState.value.copy(
                distanceMeters = dist,
                bearingDegrees = bearing,
                relativeNeedleAngle = relative,
                isArrived = arrived
            )
        } else {
            _uiState.value = _uiState.value.copy(
                distanceMeters = null,
                bearingDegrees = null,
                isArrived = false
            )
        }
    }

    private fun vibrateFeedback(longVibe: Boolean) {
        try {
            val context = getApplication<Application>()
            val duration = if (longVibe) 150L else 75L
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(duration)
            }
        } catch (e: Exception) {
            // Ignore if vibration unsupported
        }
    }

    private fun vibrateArrivalPulse() {
        try {
            val context = getApplication<Application>()
            val pattern = longArrayOf(0, 100, 100, 200, 100, 250)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationUpdatesJob?.cancel()
        compassJob?.cancel()
        simulatedDemoJob?.cancel()
    }
}
