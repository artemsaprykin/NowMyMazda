package com.example.data.model

import com.example.data.remote.ConnectionMode

enum class MazdaConnectionState {
    DISCONNECTED,
    AUTHENTICATING,
    AUTHENTICATED,
    VERIFYING_SESSION,
    DISCOVERING_VEHICLES,
    CONNECTED,
    SESSION_EXPIRED,
    CONNECTION_FAILED,
    UNSUPPORTED
}

enum class EngineState {
    OFF,
    STARTING,
    RUNNING,
    STOPPING,
    UNKNOWN
}

enum class LockState {
    LOCKED,
    UNLOCKED,
    UNKNOWN
}

enum class PressureStatus {
    NORMAL,
    LOW,
    CRITICAL,
    UNKNOWN
}

enum class LocationSource {
    VEHICLE_TCU,
    MOBILE_DEVICE,
    UNAVAILABLE
}

enum class UnitSystem {
    IMPERIAL, // miles, psi, °F
    METRIC    // km, bar/kPa, °C
}

data class TireInfo(
    val id: String,
    val name: String,
    val currentPsi: Double = 0.0,
    val recommendedPsi: Double = 36.0,
    val tempF: Int = 0,
    val status: PressureStatus = PressureStatus.UNKNOWN,
    val isAvailable: Boolean = false
)

data class TPMSData(
    val isAvailable: Boolean = false,
    val frontLeft: TireInfo = TireInfo("FL", "Front Left"),
    val frontRight: TireInfo = TireInfo("FR", "Front Right"),
    val rearLeft: TireInfo = TireInfo("RL", "Rear Left"),
    val rearRight: TireInfo = TireInfo("RR", "Rear Right")
)

data class BatteryHealth(
    val isAvailable: Boolean = false,
    val voltage: Double = 0.0,
    val stateOfHealthPercent: Int = 0,
    val stateOfChargePercent: Int = 0,
    val temperatureF: Int = 0,
    val alternatorStatus: String = "Unavailable",
    val healthSummary: String = "No telemetry received"
)

data class DoorState(
    val isAvailable: Boolean = false,
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
    val heatedSeatsEnabled: Boolean = false,
    val rearDefrostEnabled: Boolean = false
)

data class VehicleGpsLocation(
    val isAvailable: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "Location Unavailable",
    val lastUpdatedTimestamp: Long? = null,
    val isParked: Boolean = true,
    val source: LocationSource = LocationSource.UNAVAILABLE
)

data class GeofenceSettings(
    val isEnabled: Boolean = false,
    val centerLatitude: Double = 0.0,
    val centerLongitude: Double = 0.0,
    val radiusMeters: Int = 300,
    val safeZoneName: String = "Safe Zone",
    val isBreached: Boolean = false,
    val theftAlarmActive: Boolean = false
)

data class DiagnosticStatus(
    val isAvailable: Boolean = false,
    val checkEngineOn: Boolean = false,
    val oilLifePercent: Int = 0,
    val nextServiceMiles: Int = 0,
    val brakePadsPercent: Int = 0,
    val brakeFluidOk: Boolean = true,
    val washerFluidLow: Boolean = false,
    val activeDtcCodes: List<String> = emptyList(),
    val lastScanTimestamp: Long? = null,
    val scanStatusMessage: String = "No cloud telematics report received"
)

data class CloudApiConfig(
    val isConnected: Boolean = false,
    val connectionState: MazdaConnectionState = MazdaConnectionState.DISCONNECTED,
    val connectionMode: ConnectionMode = ConnectionMode.DIRECT_MAZDA,
    val apiUrl: String = "https://0cxo7m58.mazda.com/prod/",
    val vin: String = "",
    val hasValidSession: Boolean = false,
    val sessionExpiryTimestamp: Long = 0L,
    val sessionStateSummary: String = "Disconnected",
    val lastSyncTimestamp: Long = 0L,
    val cellularSignalBars: Int = 0,
    val isSyncing: Boolean = false,
    val lastHttpCode: Int? = null,
    val lastLatencyMs: Long = 0L,
    val lastErrorMessage: String? = null,
    val lastExecutedCommand: String? = null,
    val regionEndpoint: String = "North America (MNAO)",
    val isGatewayAdapter: Boolean = false
)

data class ActiveDriveSession(
    val startTimeMillis: Long = System.currentTimeMillis(),
    val startAddress: String = "",
    val destinationAddress: String = "",
    val currentDistanceMiles: Double = 0.0,
    val durationSeconds: Int = 0,
    val currentSpeedMph: Double = 0.0,
    val maxSpeedMph: Double = 0.0,
    val instantMpg: Double = 0.0,
    val currentStepIndex: Int = 0,
    val currentRouteLeg: String = "",
    val routeLegs: List<String> = emptyList(),
    val drivingScore: Int = 100,
    val tripTag: String = "General"
)

data class DiscoveredVehicle(
    val vin: String,
    val modelName: String,
    val modelYear: Int,
    val nickname: String,
    val isTcuPaired: Boolean = false,
    val canRemoteControl: Boolean = false,
    val isSelected: Boolean = false
)

data class MyMazdaAccount(
    val email: String = "",
    val isLinked: Boolean = false,
    val connectionState: MazdaConnectionState = MazdaConnectionState.DISCONNECTED,
    val connectionMode: ConnectionMode = ConnectionMode.DIRECT_MAZDA,
    val region: String = "North America (MNAO)",
    val regionCode: String = "MNAO",
    val customerId: String = "",
    val vehicleCount: Int = 0,
    val lastLoginTimestamp: Long = 0L,
    val isTcuPaired: Boolean = false,
    val vehicleVin: String = "",
    val discoveredVehicles: List<DiscoveredVehicle> = emptyList(),
    val dataSharingAgreementGranted: Boolean = false,
    val remoteControlEntitlementActive: Boolean = false,
    val inCarVerificationCompleted: Boolean = false,
    val safetyInterlocksClear: Boolean = false,
    val lastHandshakeStatus: String = "Not connected"
)

sealed class CommandExecutionState {
    object Idle : CommandExecutionState()
    data class Submitting(val action: String, val message: String = "Authorizing command...") : CommandExecutionState()
    data class Accepted(val action: String, val message: String = "Command accepted by Mazda cloud. Awaiting TCU execution...", val commandId: String? = null) : CommandExecutionState()
    data class Pending(val action: String, val message: String = "Sending request to Mazda Connected Services...") : CommandExecutionState()
    data class Confirmed(val action: String, val message: String, val commandId: String? = null) : CommandExecutionState()
    data class Failed(val action: String, val errorMessage: String, val errorCode: String? = null) : CommandExecutionState()
    data class TimedOut(val action: String, val errorMessage: String = "Command timed out waiting for vehicle TCU response.") : CommandExecutionState()
    data class UnknownOutcome(val action: String, val message: String) : CommandExecutionState()
    data class Unsupported(val action: String, val reason: String = "Command not supported on this vehicle.") : CommandExecutionState()
}

data class VehicleState(
    val vin: String = "",
    val modelName: String = "No Vehicle Connected",
    val year: Int = 0,
    val nickname: String = "Not Connected",
    val isConnected: Boolean = false,
    val connectionState: MazdaConnectionState = MazdaConnectionState.DISCONNECTED,
    val isVehicleSelected: Boolean = false,
    val remoteControlEnabled: Boolean = false, // Kept disabled until authenticated read-only vehicle communication is verified
    val engineState: EngineState = EngineState.UNKNOWN,
    val lockState: LockState = LockState.UNKNOWN,
    val hazardLightsActive: Boolean = false,
    val hornActive: Boolean = false,
    val odometerMiles: Int = 0,
    val fuelPercent: Int = 0,
    val estimatedRangeMiles: Int = 0,
    val isOdometerAvailable: Boolean = false,
    val isFuelAvailable: Boolean = false,
    val lastReportTimestamp: Long? = null,
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
    val commandExecutionState: CommandExecutionState = CommandExecutionState.Idle,
    val themeMode: com.example.ui.theme.ThemeMode = com.example.ui.theme.ThemeMode.DARK,
    val unitSystem: UnitSystem = UnitSystem.IMPERIAL
)
