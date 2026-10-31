package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.VehicleRepository
import com.example.data.local.MaintenanceEntity
import com.example.data.local.TripLogEntity
import com.example.data.model.LockState
import com.example.data.model.VehicleState
import com.example.ui.theme.ThemeMode
import com.example.util.BiometricSecurityConfig
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class VehicleNavTab {
    DASHBOARD,
    DIAGNOSTICS,
    LOCATE,
    TRIPS,
    SETTINGS,
    CONNECTION
}

enum class TripSortOption(val displayName: String) {
    NEWEST("Most Recent"),
    OLDEST("Oldest First"),
    DISTANCE_DESC("Longest Distance"),
    DISTANCE_ASC("Shortest Distance"),
    MPG_DESC("Best Fuel Economy"),
    SCORE_DESC("Best Drive Score");

    val label: String get() = displayName
}

sealed class PendingSecurityAction(val title: String, val description: String) {
    data class RemoteEngineStart(val durationMinutes: Int, val tempF: Int) :
        PendingSecurityAction("Authorize Remote Engine Start", "Biometric verification required to ignite vehicle engine remotely.")
    object RemoteDoorUnlock :
        PendingSecurityAction("Authorize Door Unlock", "Biometric verification required to unlock vehicle doors remotely.")
    object DismissTheftAlarm :
        PendingSecurityAction("Dismiss Vehicle Theft Alarm", "Biometric verification required to silence vehicle siren and lights.")
}

class VehicleViewModel(application: Application) : AndroidViewModel(application) {
    val repository = VehicleRepository(application, viewModelScope)

    val vehicleState: StateFlow<VehicleState> = repository.vehicleState

    val tripLogs: StateFlow<List<TripLogEntity>> = repository.getAllTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tripSearchQuery = MutableStateFlow("")
    val selectedTripTagFilter = MutableStateFlow("All")
    val selectedSortOrder = MutableStateFlow(TripSortOption.NEWEST)

    val filteredAndSortedTrips: StateFlow<List<TripLogEntity>> = combine(
        tripLogs,
        tripSearchQuery,
        selectedTripTagFilter,
        selectedSortOrder
    ) { trips, query, tag, sort ->
        trips.filter { trip ->
            val matchesTag = tag == "All" || trip.tripTag.equals(tag, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    trip.startAddress.contains(query, ignoreCase = true) ||
                    trip.endAddress.contains(query, ignoreCase = true) ||
                    trip.routeSummary.contains(query, ignoreCase = true) ||
                    trip.tripTag.contains(query, ignoreCase = true)
            matchesTag && matchesQuery
        }.let { list ->
            when (sort) {
                TripSortOption.NEWEST -> list.sortedByDescending { it.startTimeMillis }
                TripSortOption.OLDEST -> list.sortedBy { it.startTimeMillis }
                TripSortOption.DISTANCE_DESC -> list.sortedByDescending { it.distanceMiles }
                TripSortOption.DISTANCE_ASC -> list.sortedBy { it.distanceMiles }
                TripSortOption.MPG_DESC -> list.sortedByDescending { it.avgMpg }
                TripSortOption.SCORE_DESC -> list.sortedByDescending { it.drivingScore }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val maintenanceReminders: StateFlow<List<MaintenanceEntity>> = repository.getMaintenanceRemindersForVin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTab = MutableStateFlow(VehicleNavTab.DASHBOARD)
    val currentTab = _currentTab.asStateFlow()

    private val _securityConfig = MutableStateFlow(BiometricSecurityConfig())
    val securityConfig = _securityConfig.asStateFlow()

    private val _pendingSecurityAction = MutableStateFlow<PendingSecurityAction?>(null)
    val pendingSecurityAction = _pendingSecurityAction.asStateFlow()

    private val _biometricErrorMessage = MutableStateFlow<String?>(null)
    val biometricErrorMessage = _biometricErrorMessage.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode = _themeMode.asStateFlow()

    private val _unitSystem = MutableStateFlow(com.example.data.model.UnitSystem.IMPERIAL)
    val unitSystem = _unitSystem.asStateFlow()

    private val _userGpsLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userGpsLocation = _userGpsLocation.asStateFlow()

    init {
        NotificationHelper.createNotificationChannel(application)

        // Observe DataStore settings and keep StateFlows reactively updated
        viewModelScope.launch {
            repository.preferencesManager.settingsFlow.collect { settings ->
                _themeMode.value = settings.themeMode
                _unitSystem.value = settings.unitSystem
                _securityConfig.value = BiometricSecurityConfig(
                    requireForRemoteStart = settings.requireBiometricForRemoteStart,
                    requireForDoorUnlock = settings.requireBiometricForDoorUnlock,
                    requireForTheftAlarm = settings.requireBiometricForTheftAlarm,
                    requireForAppLaunch = settings.requireBiometricForAppLaunch,
                    hasConfiguredPin = settings.hasCustomPin
                )
            }
        }
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
            if (_securityConfig.value.requireForDoorUnlock) {
                _pendingSecurityAction.value = PendingSecurityAction.RemoteDoorUnlock
            } else {
                repository.toggleDoorsLock()
            }
        } else {
            repository.toggleDoorsLock()
        }
    }

    fun requestStopEngine() {
        repository.requestStopEngine()
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

    fun requestTpmsDiagnosticScan() {
        repository.requestTpmsDiagnosticScan()
    }

    fun runBatteryLoadTest() {
        repository.runBatteryLoadTest()
    }

    fun runDtcDiagnosticScan() {
        repository.runDtcDiagnosticScan()
    }

    fun clearDtcCodes() {
        repository.clearDtcCodes()
    }

    fun refreshVehicleGpsLocation() {
        repository.refreshVehicleGpsLocation()
    }

    fun setSafeZoneCurrent() {
        repository.setVehicleLocationAsSafeZone()
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
        viewModelScope.launch {
            repository.preferencesManager.setBiometricPreferences(
                forRemoteStart = config.requireForRemoteStart,
                forDoorUnlock = config.requireForDoorUnlock,
                forTheftAlarm = config.requireForTheftAlarm,
                forAppLaunch = config.requireForAppLaunch
            )
        }
    }

    fun setUserPin(pin: String) {
        viewModelScope.launch {
            repository.preferencesManager.setUserPin(pin)
        }
    }

    fun authenticateWithBiometricsSuccess() {
        executePendingAction()
    }

    fun authenticateWithPin(pin: String) {
        viewModelScope.launch {
            val isValid = repository.preferencesManager.verifyPin(pin)
            if (isValid) {
                _biometricErrorMessage.value = null
                executePendingAction()
            } else {
                _biometricErrorMessage.value = "Incorrect Security PIN. Please enter your configured 4-digit PIN."
            }
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

    fun setTripSearchQuery(query: String) {
        tripSearchQuery.value = query
    }

    fun setTripTagFilter(tag: String) {
        selectedTripTagFilter.value = tag
    }

    fun setTripSortOrder(sort: TripSortOption) {
        selectedSortOrder.value = sort
    }

    fun startLiveDrive(start: String, dest: String, tag: String) {
        repository.startLiveDrive(start, dest, tag)
    }

    fun deleteTrip(tripId: Long) {
        viewModelScope.launch {
            repository.deleteTrip(tripId)
        }
    }

    fun clearAllTrips() {
        viewModelScope.launch {
            repository.clearAllTrips()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.preferencesManager.setThemeMode(mode)
        }
    }

    fun setUnitSystem(units: com.example.data.model.UnitSystem) {
        viewModelScope.launch {
            repository.preferencesManager.setUnitSystem(units)
        }
    }

    fun saveRemoteStartPreferences(durationMinutes: Int, targetTempF: Int, heatedSeats: Boolean = false, rearDefrost: Boolean = false) {
        viewModelScope.launch {
            repository.preferencesManager.saveRemoteStartPreferences(durationMinutes, targetTempF, heatedSeats, rearDefrost)
        }
    }

    fun saveNotificationPreferences(lowFuel: Boolean, unlockedDoor: Boolean, geofenceBreach: Boolean) {
        viewModelScope.launch {
            repository.preferencesManager.setNotificationPreferences(lowFuel, unlockedDoor, geofenceBreach)
        }
    }

    fun authenticateWithMazdaConnectedServices(
        email: String,
        password: String,
        regionCode: String,
        customBaseUrl: String? = null,
        onComplete: (Boolean, String) -> Unit
    ) {
        val mode = if (regionCode == "BRIDGE" || !customBaseUrl.isNullOrBlank()) {
            com.example.data.remote.ConnectionMode.MCS_GATEWAY
        } else {
            com.example.data.remote.ConnectionMode.DIRECT_MAZDA
        }
        authenticateWithMazdaConnectedServices(email, password, regionCode, customBaseUrl, mode, onComplete)
    }

    fun authenticateWithMazdaConnectedServices(
        email: String,
        password: String,
        regionCode: String,
        customBaseUrl: String?,
        connectionMode: com.example.data.remote.ConnectionMode,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.authenticateWithMazdaConnectedServices(email, password, regionCode, customBaseUrl, connectionMode)
            result.fold(
                onSuccess = { list ->
                    onComplete(true, "Successfully authenticated with Mazda Connected Services. Found ${list.size} vehicle(s).")
                },
                onFailure = { error ->
                    onComplete(false, error.localizedMessage ?: "Failed to connect to Mazda cloud")
                }
            )
        }
    }

    fun selectActiveVehicle(vin: String) {
        repository.selectActiveVehicle(vin)
    }

    fun disconnectMazdaAccount() {
        repository.disconnectMazdaAccount()
    }

    fun testConnectionHandshake(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val summary = repository.testConnectionHandshake()
            onResult(summary)
        }
    }

    fun updateUserLocation(lat: Double, lng: Double) {
        _userGpsLocation.value = Pair(lat, lng)
    }
}
