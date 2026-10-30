package com.example.data.model

enum class EngineState {
    OFF,
    STARTING,
    RUNNING,
    STOPPING
}

enum class LockState {
    LOCKED,
    UNLOCKED
}

enum class PressureStatus {
    NORMAL,
    LOW,
    CRITICAL
}

data class TireInfo(
    val id: String,
    val name: String,
    val currentPsi: Double,
    val recommendedPsi: Double = 36.0,
    val tempF: Int = 74,
    val status: PressureStatus = PressureStatus.NORMAL
)

data class TPMSData(
    val frontLeft: TireInfo = TireInfo("FL", "Front Left", 36.0),
    val frontRight: TireInfo = TireInfo("FR", "Front Right", 36.0),
    val rearLeft: TireInfo = TireInfo("RL", "Rear Left", 36.0),
    val rearRight: TireInfo = TireInfo("RR", "Rear Right", 35.5)
)

data class BatteryHealth(
    val voltage: Double = 12.6,
    val stateOfHealthPercent: Int = 96,
    val stateOfChargePercent: Int = 88,
    val temperatureF: Int = 72,
    val alternatorStatus: String = "14.2V (Charging OK)",
    val healthSummary: String = "Good Condition"
)

data class DoorState(
    val isDriverOpen: Boolean = false,
    val isPassengerOpen: Boolean = false,
    val isRearLeftOpen: Boolean = false,
    val isRearRightOpen: Boolean = false,
    val isTrunkOpen: Boolean = false,
    val isHoodOpen: Boolean = false,
    val areWindowsOpen: Boolean = false
)

data class RemoteStartSettings(
    val runDurationMinutes: Int = 15,
    val remainingSeconds: Int = 0,
    val targetTempF: Int = 72,
    val heatedSeatsEnabled: Boolean = true,
    val rearDefrostEnabled: Boolean = true
)

data class VehicleGpsLocation(
    val latitude: Double = 47.6062,
    val longitude: Double = -122.3321,
    val address: String = "Pine St & 4th Ave, Seattle, WA",
    val lastUpdatedTimestamp: Long = System.currentTimeMillis(),
    val isParked: Boolean = true
)

data class GeofenceSettings(
    val isEnabled: Boolean = true,
    val centerLatitude: Double = 47.6062,
    val centerLongitude: Double = -122.3321,
    val radiusMeters: Int = 300,
    val safeZoneName: String = "Home Garage",
    val isBreached: Boolean = false,
    val theftAlarmActive: Boolean = false
)

data class DiagnosticStatus(
    val checkEngineOn: Boolean = false,
    val oilLifePercent: Int = 68,
    val nextServiceMiles: Int = 3180,
    val brakePadsPercent: Int = 82,
    val brakeFluidOk: Boolean = true,
    val washerFluidLow: Boolean = false,
    val activeDtcCodes: List<String> = emptyList(),
    val lastScanTimestamp: Long = System.currentTimeMillis(),
    val scanStatusMessage: String = "All ECU modules report zero pending DTCs (Clean OBD-II Status)"
)

data class CloudApiConfig(
    val isConnected: Boolean = true,
    val apiUrl: String = "https://connected-api.mazda.cloud/v2/telematics",
    val vin: String = "JM3KFBDM9P0128934",
    val apiToken: String = "mzd_live_sec_892348a0f9b31",
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val cellularSignalBars: Int = 4,
    val isSyncing: Boolean = false,
    val lastHttpCode: Int? = 200,
    val lastLatencyMs: Long = 184,
    val lastErrorMessage: String? = null,
    val lastExecutedCommand: String? = "Telematics Polling (OK)",
    val regionEndpoint: String = "North America (MNAO)"
)

data class ActiveDriveSession(
    val startTimeMillis: Long = System.currentTimeMillis(),
    val startAddress: String = "Home Garage, Seattle",
    val destinationAddress: String = "Bellevue Tech Center, WA",
    val currentDistanceMiles: Double = 0.0,
    val durationSeconds: Int = 0,
    val currentSpeedMph: Double = 0.0,
    val maxSpeedMph: Double = 0.0,
    val instantMpg: Double = 28.5,
    val currentStepIndex: Int = 0,
    val currentRouteLeg: String = "Departing origin",
    val routeLegs: List<String> = listOf(
        "Departing origin",
        "Merging onto Interstate 5",
        "Express lanes cruising",
        "Exit 14 toward Bellevue Way",
        "Arriving at destination"
    ),
    val drivingScore: Int = 95,
    val tripTag: String = "Commute"
)

data class MyMazdaAccount(
    val email: String = "metra2007@meta.ua",
    val isLinked: Boolean = true,
    val region: String = "North America (MNAO)",
    val customerId: String = "MZD-8923019",
    val vehicleCount: Int = 1,
    val lastLoginTimestamp: Long = System.currentTimeMillis(),
    val isTcuPaired: Boolean = true,
    val vehicleVin: String = "JM3KFBDM9P0128934",
    val sessionToken: String = "mzd_live_sec_892348a0f9b31",
    val tokenExpiresAt: Long = System.currentTimeMillis() + 86400_000L,
    val remoteStartPermitted: Boolean = true,
    val doorLockPermitted: Boolean = true,
    val telematicsPermitted: Boolean = true,
    val gpsTrackingPermitted: Boolean = true,
    val dtcDiagnosticsPermitted: Boolean = true
)

data class VehicleState(
    val vin: String = "JM3KFBDM9P0128934",
    val modelName: String = "Mazda CX-50 Turbo AWD",
    val year: Int = 2024,
    val nickname: String = "Soul Red CX-50",
    val engineState: EngineState = EngineState.OFF,
    val lockState: LockState = LockState.LOCKED,
    val hazardLightsActive: Boolean = false,
    val hornActive: Boolean = false,
    val odometerMiles: Int = 14820,
    val fuelPercent: Int = 76,
    val estimatedRangeMiles: Int = 345,
    val doors: DoorState = DoorState(),
    val batteryHealth: BatteryHealth = BatteryHealth(),
    val tpms: TPMSData = TPMSData(),
    val diagnostics: DiagnosticStatus = DiagnosticStatus(),
    val remoteStart: RemoteStartSettings = RemoteStartSettings(),
    val location: VehicleGpsLocation = VehicleGpsLocation(),
    val geofence: GeofenceSettings = GeofenceSettings(),
    val cloudConfig: CloudApiConfig = CloudApiConfig(),
    val activeDriveSession: ActiveDriveSession? = null,
    val myMazdaAccount: MyMazdaAccount = MyMazdaAccount(),
    val themeMode: com.example.ui.theme.ThemeMode = com.example.ui.theme.ThemeMode.DARK
)
