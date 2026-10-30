package com.example.data

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.MaintenanceEntity
import com.example.data.local.TripLogEntity
import com.example.data.model.BatteryHealth
import com.example.data.model.CloudApiConfig
import com.example.data.model.DoorState
import com.example.data.model.EngineState
import com.example.data.model.GeofenceSettings
import com.example.data.model.LockState
import com.example.data.model.PressureStatus
import com.example.data.model.RemoteStartSettings
import com.example.data.model.TPMSData
import com.example.data.model.TireInfo
import com.example.data.model.VehicleGpsLocation
import com.example.data.model.VehicleState
import com.example.data.remote.ApiClient
import com.example.data.remote.RemoteControlRequest
import com.example.util.LocationHelper
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VehicleRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val database = AppDatabase.getInstance(context)
    private val tripDao = database.tripLogDao()
    private val maintenanceDao = database.maintenanceDao()

    private val _vehicleState = MutableStateFlow(VehicleState())
    val vehicleState = _vehicleState.asStateFlow()

    private var remoteStartTimerJob: Job? = null
    private var hazardLightTimerJob: Job? = null

    init {
        scope.launch {
            seedInitialDataIfNeeded()
        }
    }

    private suspend fun seedInitialDataIfNeeded() {
        val currentTrips = tripDao.getTripsForVin(_vehicleState.value.vin).firstOrNull()
        if (currentTrips.isNullOrEmpty()) {
            val now = System.currentTimeMillis()
            tripDao.insertTrip(
                TripLogEntity(
                    vin = _vehicleState.value.vin,
                    startTimeMillis = now - 3600_000 * 3,
                    endTimeMillis = now - 3600_000 * 2,
                    distanceMiles = 24.8,
                    durationMinutes = 38,
                    avgMpg = 29.4,
                    avgSpeedMph = 42.0,
                    drivingScore = 94,
                    startAddress = "Home Garage, Seattle",
                    endAddress = "Kirkland Tech Campus, WA",
                    tripTag = "Commute"
                )
            )
            tripDao.insertTrip(
                TripLogEntity(
                    vin = _vehicleState.value.vin,
                    startTimeMillis = now - 3600_000 * 26,
                    endTimeMillis = now - 3600_000 * 24,
                    distanceMiles = 68.2,
                    durationMinutes = 72,
                    avgMpg = 32.1,
                    avgSpeedMph = 58.5,
                    drivingScore = 91,
                    startAddress = "Seattle Downtown",
                    endAddress = "Snoqualmie Pass, WA",
                    tripTag = "Highway"
                )
            )
            tripDao.insertTrip(
                TripLogEntity(
                    vin = _vehicleState.value.vin,
                    startTimeMillis = now - 3600_000 * 50,
                    endTimeMillis = now - 3600_000 * 49,
                    distanceMiles = 8.4,
                    durationMinutes = 22,
                    avgMpg = 24.6,
                    avgSpeedMph = 23.0,
                    drivingScore = 88,
                    startAddress = "Home Garage, Seattle",
                    endAddress = "Trader Joe's, Queen Anne",
                    tripTag = "City"
                )
            )
        }

        val currentReminders = maintenanceDao.getRemindersForVin(_vehicleState.value.vin).firstOrNull()
        if (currentReminders.isNullOrEmpty()) {
            maintenanceDao.insertReminder(
                MaintenanceEntity(
                    vin = _vehicleState.value.vin,
                    title = "15,000 Mile Scheduled Inspection",
                    description = "Comprehensive factory multi-point inspection, synthetic oil & filter change, engine coolant test.",
                    dueOdometerMiles = 15000,
                    priority = "High",
                    category = "Engine"
                )
            )
            maintenanceDao.insertReminder(
                MaintenanceEntity(
                    vin = _vehicleState.value.vin,
                    title = "Tire Rotation & Balance",
                    description = "Rotate front and rear tires cross-pattern, check tread depth and calibrate TPMS.",
                    dueOdometerMiles = 15500,
                    priority = "Medium",
                    category = "Tires"
                )
            )
            maintenanceDao.insertReminder(
                MaintenanceEntity(
                    vin = _vehicleState.value.vin,
                    title = "Cabin Air Filter Replacement",
                    description = "Inspect and replace micro-particle pollen and allergen cabin air filter.",
                    dueOdometerMiles = 16000,
                    priority = "Low",
                    category = "Filters"
                )
            )
        }
    }

    fun getTrips(): Flow<List<TripLogEntity>> = tripDao.getTripsForVin(_vehicleState.value.vin)

    fun getMaintenanceReminders(): Flow<List<MaintenanceEntity>> =
        maintenanceDao.getRemindersForVin(_vehicleState.value.vin)

    suspend fun setReminderCompleted(id: Long, isCompleted: Boolean) {
        maintenanceDao.setCompleted(id, isCompleted)
    }

    suspend fun addTrip(
        distanceMiles: Double,
        durationMinutes: Int,
        avgMpg: Double,
        startAddress: String,
        endAddress: String,
        tag: String
    ) {
        val now = System.currentTimeMillis()
        val score = (85..98).random()
        val avgSpeed = if (durationMinutes > 0) (distanceMiles / (durationMinutes / 60.0)) else 30.0
        tripDao.insertTrip(
            TripLogEntity(
                vin = _vehicleState.value.vin,
                startTimeMillis = now - (durationMinutes * 60_000L),
                endTimeMillis = now,
                distanceMiles = distanceMiles,
                durationMinutes = durationMinutes,
                avgMpg = avgMpg,
                avgSpeedMph = avgSpeed,
                drivingScore = score,
                startAddress = startAddress,
                endAddress = endAddress,
                tripTag = tag
            )
        )
        // Also update vehicle odometer
        _vehicleState.value = _vehicleState.value.copy(
            odometerMiles = _vehicleState.value.odometerMiles + distanceMiles.toInt()
        )
    }

    fun remoteStartEngine(durationMinutes: Int = 15, climateTempF: Int = 72) {
        _vehicleState.value = _vehicleState.value.copy(
            engineState = EngineState.STARTING
        )
        scope.launch {
            delay(1200) // Simulated telematics radio latency
            _vehicleState.value = _vehicleState.value.copy(
                engineState = EngineState.RUNNING,
                remoteStart = _vehicleState.value.remoteStart.copy(
                    runDurationMinutes = durationMinutes,
                    remainingSeconds = durationMinutes * 60,
                    targetTempF = climateTempF
                ),
                batteryHealth = _vehicleState.value.batteryHealth.copy(
                    voltage = 14.2,
                    alternatorStatus = "14.2V (Alternator Active)"
                )
            )

            NotificationHelper.sendAlert(
                context,
                101,
                "Engine Started Remotely",
                "Your ${_vehicleState.value.nickname} engine is running. Cabin climate set to ${climateTempF}°F. Engine will auto-stop in $durationMinutes min.",
                isUrgent = false
            )

            // Start countdown timer
            remoteStartTimerJob?.cancel()
            remoteStartTimerJob = scope.launch {
                while (_vehicleState.value.engineState == EngineState.RUNNING &&
                    _vehicleState.value.remoteStart.remainingSeconds > 0
                ) {
                    delay(1000)
                    val remaining = _vehicleState.value.remoteStart.remainingSeconds - 1
                    _vehicleState.value = _vehicleState.value.copy(
                        remoteStart = _vehicleState.value.remoteStart.copy(remainingSeconds = remaining)
                    )
                    if (remaining <= 0) {
                        stopEngine()
                        NotificationHelper.sendAlert(
                            context,
                            102,
                            "Remote Start Timeout",
                            "Engine shut down after reaching $durationMinutes minutes limit.",
                            isUrgent = false
                        )
                    }
                }
            }
        }
    }

    fun stopEngine() {
        remoteStartTimerJob?.cancel()
        _vehicleState.value = _vehicleState.value.copy(
            engineState = EngineState.OFF,
            remoteStart = _vehicleState.value.remoteStart.copy(remainingSeconds = 0),
            batteryHealth = _vehicleState.value.batteryHealth.copy(
                voltage = 12.6,
                alternatorStatus = "12.6V (Resting Normal)"
            )
        )
    }

    fun toggleDoorsLock() {
        val newLock = if (_vehicleState.value.lockState == LockState.LOCKED) LockState.UNLOCKED else LockState.LOCKED
        _vehicleState.value = _vehicleState.value.copy(lockState = newLock)
        NotificationHelper.sendAlert(
            context,
            103,
            if (newLock == LockState.LOCKED) "Doors Locked" else "Doors Unlocked",
            "Vehicle ${if (newLock == LockState.LOCKED) "is secured." else "doors were unlocked remotely."}"
        )
    }

    fun toggleHazardLights() {
        val active = !_vehicleState.value.hazardLightsActive
        _vehicleState.value = _vehicleState.value.copy(hazardLightsActive = active)
        if (active) {
            hazardLightTimerJob?.cancel()
            hazardLightTimerJob = scope.launch {
                delay(10000) // Flash for 10 seconds then auto turn off
                _vehicleState.value = _vehicleState.value.copy(hazardLightsActive = false)
            }
        }
    }

    fun soundHornAndFlash() {
        _vehicleState.value = _vehicleState.value.copy(
            hornActive = true,
            hazardLightsActive = true
        )
        scope.launch {
            delay(4000)
            _vehicleState.value = _vehicleState.value.copy(
                hornActive = false,
                hazardLightsActive = false
            )
        }
    }

    fun toggleTrunk() {
        val isTrunkOpen = !_vehicleState.value.doors.isTrunkOpen
        _vehicleState.value = _vehicleState.value.copy(
            doors = _vehicleState.value.doors.copy(isTrunkOpen = isTrunkOpen)
        )
    }

    fun calibrateTpms() {
        _vehicleState.value = _vehicleState.value.copy(
            tpms = TPMSData(
                frontLeft = TireInfo("FL", "Front Left", 36.0, 36.0, 74, PressureStatus.NORMAL),
                frontRight = TireInfo("FR", "Front Right", 36.0, 36.0, 74, PressureStatus.NORMAL),
                rearLeft = TireInfo("RL", "Rear Left", 36.0, 36.0, 75, PressureStatus.NORMAL),
                rearRight = TireInfo("RR", "Rear Right", 36.0, 36.0, 75, PressureStatus.NORMAL)
            )
        )
        NotificationHelper.sendAlert(
            context,
            104,
            "TPMS Recalibrated",
            "All four tires reset to factory baseline 36.0 PSI."
        )
    }

    fun simulateTirePressureLeak() {
        // Simulates a low tire on Front Left for testing diagnostics & push alerts
        val lowFl = _vehicleState.value.tpms.frontLeft.copy(
            currentPsi = 27.5,
            status = PressureStatus.LOW
        )
        _vehicleState.value = _vehicleState.value.copy(
            tpms = _vehicleState.value.tpms.copy(frontLeft = lowFl)
        )
        NotificationHelper.sendAlert(
            context,
            105,
            "Low Tire Pressure Warning",
            "Front Left tire pressure has dropped to 27.5 PSI. Recommended: 36.0 PSI.",
            isUrgent = true
        )
    }

    fun simulateBatteryDrain() {
        val lowBattery = _vehicleState.value.batteryHealth.copy(
            voltage = 11.7,
            stateOfChargePercent = 42,
            stateOfHealthPercent = 78,
            healthSummary = "Low Charge - Recharging Recommended"
        )
        _vehicleState.value = _vehicleState.value.copy(batteryHealth = lowBattery)
        NotificationHelper.sendAlert(
            context,
            106,
            "12V Battery Health Warning",
            "Battery voltage is at 11.7V. Start vehicle or connect charger to prevent discharge.",
            isUrgent = true
        )
    }

    fun restoreHealthyBattery() {
        _vehicleState.value = _vehicleState.value.copy(
            batteryHealth = BatteryHealth(
                voltage = 12.6,
                stateOfHealthPercent = 96,
                stateOfChargePercent = 88,
                temperatureF = 72,
                alternatorStatus = "14.2V (Charging OK)",
                healthSummary = "Good Condition"
            )
        )
    }

    fun updateGeofence(enabled: Boolean, radiusMeters: Int, safeZoneName: String) {
        _vehicleState.value = _vehicleState.value.copy(
            geofence = _vehicleState.value.geofence.copy(
                isEnabled = enabled,
                radiusMeters = radiusMeters,
                safeZoneName = safeZoneName
            )
        )
    }

    fun setVehicleLocationAsSafeZone() {
        val loc = _vehicleState.value.location
        _vehicleState.value = _vehicleState.value.copy(
            geofence = _vehicleState.value.geofence.copy(
                centerLatitude = loc.latitude,
                centerLongitude = loc.longitude,
                isBreached = false,
                theftAlarmActive = false
            )
        )
    }

    fun simulateGeofenceBreach() {
        // Move vehicle slightly away outside radius
        val breachedLocation = _vehicleState.value.location.copy(
            latitude = _vehicleState.value.location.latitude + 0.008,
            longitude = _vehicleState.value.location.longitude + 0.009,
            address = "Mercer St & Fairview Ave N, Seattle, WA",
            isParked = false
        )
        _vehicleState.value = _vehicleState.value.copy(
            location = breachedLocation,
            geofence = _vehicleState.value.geofence.copy(
                isBreached = true,
                theftAlarmActive = true
            ),
            hazardLightsActive = true
        )
        NotificationHelper.sendAlert(
            context,
            107,
            "🚨 GEOFENCE THEFT ALERT!",
            "Vehicle exited '${_vehicleState.value.geofence.safeZoneName}' boundary! GPS tracking active.",
            isUrgent = true
        )
    }

    fun dismissTheftAlarm() {
        _vehicleState.value = _vehicleState.value.copy(
            geofence = _vehicleState.value.geofence.copy(
                isBreached = false,
                theftAlarmActive = false
            ),
            hazardLightsActive = false
        )
    }

    fun updateCloudConfig(apiUrl: String, token: String) {
        _vehicleState.value = _vehicleState.value.copy(
            cloudConfig = _vehicleState.value.cloudConfig.copy(
                apiUrl = apiUrl,
                apiToken = token
            )
        )
    }

    suspend fun syncWithCloudApi(): Boolean {
        _vehicleState.value = _vehicleState.value.copy(
            cloudConfig = _vehicleState.value.cloudConfig.copy(isSyncing = true)
        )
        return withContext(Dispatchers.IO) {
            try {
                // Try remote API call or simulate verified response
                delay(900)
                _vehicleState.value = _vehicleState.value.copy(
                    cloudConfig = _vehicleState.value.cloudConfig.copy(
                        isSyncing = false,
                        isConnected = true,
                        lastSyncTimestamp = System.currentTimeMillis()
                    )
                )
                true
            } catch (e: Exception) {
                _vehicleState.value = _vehicleState.value.copy(
                    cloudConfig = _vehicleState.value.cloudConfig.copy(isSyncing = false)
                )
                false
            }
        }
    }
}
