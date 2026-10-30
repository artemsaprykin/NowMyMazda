package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VehicleRepository
import com.example.data.local.MaintenanceEntity
import com.example.data.local.TripLogEntity
import com.example.data.model.EngineState
import com.example.data.model.LockState
import com.example.data.model.VehicleState
import com.example.util.BiometricSecurityConfig
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class VehicleNavTab {
    DASHBOARD,
    DIAGNOSTICS,
    LOCATE,
    TRIPS,
    SETTINGS
}

sealed class PendingSecurityAction(val title: String, val description: String) {
    data class RemoteEngineStart(val durationMinutes: Int, val tempF: Int) :
        PendingSecurityAction("Confirm Remote Engine Start", "Authorize remote ignition of the engine via biometric verification.")

    data object RemoteDoorUnlock :
        PendingSecurityAction("Confirm Remote Door Unlock", "Authorize vehicle unlock command.")

    data object DismissTheftAlarm :
        PendingSecurityAction("Deactivate Theft Alarm", "Authenticate to silence alarm and disarm security alert.")
}

class VehicleViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VehicleRepository(application, viewModelScope)

    val vehicleState: StateFlow<VehicleState> = repository.vehicleState

    val tripLogs: StateFlow<List<TripLogEntity>> = repository.getTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val maintenanceReminders: StateFlow<List<MaintenanceEntity>> = repository.getMaintenanceReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTab = MutableStateFlow(VehicleNavTab.DASHBOARD)
    val currentTab = _currentTab.asStateFlow()

    private val _securityConfig = MutableStateFlow(BiometricSecurityConfig())
    val securityConfig = _securityConfig.asStateFlow()

    private val _pendingSecurityAction = MutableStateFlow<PendingSecurityAction?>(null)
    val pendingSecurityAction = _pendingSecurityAction.asStateFlow()

    private val _biometricErrorMessage = MutableStateFlow<String?>(null)
    val biometricErrorMessage = _biometricErrorMessage.asStateFlow()

    private val _userGpsLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userGpsLocation = _userGpsLocation.asStateFlow()

    init {
        NotificationHelper.createNotificationChannel(application)
    }

    fun selectTab(tab: VehicleNavTab) {
        _currentTab.value = tab
    }

    fun requestRemoteStart(durationMinutes: Int = 15, tempF: Int = 72) {
        if (_securityConfig.value.requireForRemoteStart) {
            _pendingSecurityAction.value = PendingSecurityAction.RemoteEngineStart(durationMinutes, tempF)
        } else {
            repository.remoteStartEngine(durationMinutes, tempF)
        }
    }

    fun requestDoorToggle() {
        val isLocked = vehicleState.value.lockState == LockState.LOCKED
        if (isLocked) {
            // Unlocking requires biometric check if configured
            if (_securityConfig.value.requireForDoorUnlock) {
                _pendingSecurityAction.value = PendingSecurityAction.RemoteDoorUnlock
            } else {
                repository.toggleDoorsLock()
            }
        } else {
            // Locking does not require biometric
            repository.toggleDoorsLock()
        }
    }

    fun requestStopEngine() {
        repository.stopEngine()
    }

    fun toggleHazardLights() {
        repository.toggleHazardLights()
    }

    fun panicHorn() {
        repository.soundHornAndFlash()
    }

    fun toggleTrunk() {
        repository.toggleTrunk()
    }

    fun calibrateTpms() {
        repository.calibrateTpms()
    }

    fun simulateTirePressureLeak() {
        repository.simulateTirePressureLeak()
    }

    fun simulateBatteryDrain() {
        repository.simulateBatteryDrain()
    }

    fun restoreHealthyBattery() {
        repository.restoreHealthyBattery()
    }

    fun setSafeZoneCurrent() {
        repository.setVehicleLocationAsSafeZone()
    }

    fun simulateGeofenceBreach() {
        repository.simulateGeofenceBreach()
    }

    fun requestDismissTheftAlarm() {
        if (_securityConfig.value.requireForTheftAlarm) {
            _pendingSecurityAction.value = PendingSecurityAction.DismissTheftAlarm
        } else {
            repository.dismissTheftAlarm()
        }
    }

    fun updateGeofenceRadius(radiusMeters: Int) {
        repository.updateGeofence(
            enabled = vehicleState.value.geofence.isEnabled,
            radiusMeters = radiusMeters,
            safeZoneName = vehicleState.value.geofence.safeZoneName
        )
    }

    fun toggleGeofence(enabled: Boolean) {
        repository.updateGeofence(
            enabled = enabled,
            radiusMeters = vehicleState.value.geofence.radiusMeters,
            safeZoneName = vehicleState.value.geofence.safeZoneName
        )
    }

    fun syncCloudApi() {
        viewModelScope.launch {
            repository.syncWithCloudApi()
        }
    }

    fun updateCloudConfig(apiUrl: String, token: String) {
        repository.updateCloudConfig(apiUrl, token)
    }

    fun setReminderCompleted(id: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.setReminderCompleted(id, isCompleted)
        }
    }

    fun addManualTrip(distance: Double, duration: Int, mpg: Double, start: String, end: String, tag: String) {
        viewModelScope.launch {
            repository.addTrip(distance, duration, mpg, start, end, tag)
        }
    }

    fun updateSecurityConfig(config: BiometricSecurityConfig) {
        _securityConfig.value = config
    }

    fun authenticateWithBiometricsSuccess() {
        executePendingAction()
    }

    fun authenticateWithPin(pin: String): Boolean {
        if (pin == _securityConfig.value.userPin) {
            _biometricErrorMessage.value = null
            executePendingAction()
            return true
        } else {
            _biometricErrorMessage.value = "Incorrect PIN. Default is 1920"
            return false
        }
    }

    fun cancelPendingAction() {
        _pendingSecurityAction.value = null
        _biometricErrorMessage.value = null
    }

    private fun executePendingAction() {
        when (val action = _pendingSecurityAction.value) {
            is PendingSecurityAction.RemoteEngineStart -> {
                repository.remoteStartEngine(action.durationMinutes, action.tempF)
            }
            is PendingSecurityAction.RemoteDoorUnlock -> {
                repository.toggleDoorsLock()
            }
            is PendingSecurityAction.DismissTheftAlarm -> {
                repository.dismissTheftAlarm()
            }
            null -> {}
        }
        _pendingSecurityAction.value = null
        _biometricErrorMessage.value = null
    }

    fun updateUserLocation(lat: Double, lng: Double) {
        _userGpsLocation.value = Pair(lat, lng)
    }
}
