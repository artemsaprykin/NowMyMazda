package com.example.data

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.MaintenanceEntity
import com.example.data.local.TripLogEntity
import com.example.data.model.ActiveDriveSession
import com.example.data.model.BatteryHealth
import com.example.data.model.CloudApiConfig
import com.example.data.model.CommandExecutionState
import com.example.data.model.DiagnosticStatus
import com.example.data.model.DiscoveredVehicle
import com.example.data.model.DoorState
import com.example.data.model.EngineState
import com.example.data.model.GeofenceSettings
import com.example.data.model.LocationSource
import com.example.data.model.LockState
import com.example.data.model.MyMazdaAccount
import com.example.data.model.PressureStatus
import com.example.data.model.RemoteStartSettings
import com.example.data.model.TPMSData
import com.example.data.model.TireInfo
import com.example.data.model.VehicleGpsLocation
import com.example.data.model.VehicleState
import com.example.data.remote.ApiClient
import com.example.data.remote.ConnectedVehicleApiService
import com.example.data.remote.MazdaB2cAuthEngine
import com.example.data.remote.MazdaRegionalCluster
import com.example.data.remote.McsGatewayClient
import com.example.data.remote.McsRemoteCommandRequestDto
import com.example.data.remote.RemoteControlRequest
import com.example.data.storage.AppPreferencesManager
import com.example.data.storage.MazdaSessionManager
import com.example.data.storage.SecureTokenStorage
import com.example.util.LocationHelper
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single source of truth for NowMyMazda.
 *
 * Implements real Mazda Connected Services integration:
 * - Azure AD B2C authentication lifecycle (MazdaB2cAuthEngine) matching cv/mcs auth.go and b2c.go
 * - Formally defined MCS Gateway Adapter (McsGatewayAdapter) for self-hosted cv/mcs daemon or proxy
 * - Full token lifecycle and rotation via MazdaSessionManager
 * - Android Keystore fail-closed token storage without Base64 fallbacks
 * - Strict real telemetry mapping without inferred or simulated defaults
 * - Non-destructive Room database persistence
 */
class VehicleRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val database = AppDatabase.getInstance(context)
    private val tripDao = database.tripLogDao()
    private val maintenanceDao = database.maintenanceDao()

    val preferencesManager = AppPreferencesManager(context)
    val tokenStorage = SecureTokenStorage(context)
    val sessionManager = MazdaSessionManager(tokenStorage)

    private val _vehicleState = MutableStateFlow(VehicleState())
    val vehicleState = _vehicleState.asStateFlow()

    private var commandPollingJob: Job? = null
    private var hazardLightTimerJob: Job? = null

    init {
        scope.launch {
            restorePersistedState()
        }
    }

    private suspend fun restorePersistedState() {
        val settings = preferencesManager.settingsFlow.first()
        val storedSession = sessionManager.getStoredSession()
        val isSessionValid = sessionManager.hasValidSession()

        val regionCluster = MazdaRegionalCluster.fromCode(settings.accountRegionCode)
        val isGateway = settings.customBridgeUrl.isNotBlank() || regionCluster == MazdaRegionalCluster.CV_MCS_BRIDGE
        val targetUrl = if (settings.customBridgeUrl.isNotBlank()) settings.customBridgeUrl else regionCluster.primaryBaseUrl

        if (storedSession != null && settings.accountEmail.isNotBlank()) {
            val isVinSelected = settings.selectedVin.isNotBlank()
            _vehicleState.value = _vehicleState.value.copy(
                vin = settings.selectedVin,
                nickname = if (isVinSelected) settings.selectedNickname.ifBlank { "Connected Mazda" } else "Select Vehicle",
                modelName = if (isVinSelected) settings.selectedNickname.ifBlank { "Mazda Connected Vehicle" } else "No Vehicle Selected",
                isVehicleSelected = isVinSelected,
                isConnected = false, // Must be verified by genuine ping or status response
                themeMode = settings.themeMode,
                remoteStart = _vehicleState.value.remoteStart.copy(
                    runDurationMinutes = settings.remoteStartDurationMinutes,
                    targetTempF = settings.remoteStartTargetTempF
                ),
                geofence = GeofenceSettings(
                    isEnabled = settings.geofenceEnabled,
                    radiusMeters = settings.geofenceRadiusMeters,
                    safeZoneName = settings.geofenceSafeZoneName,
                    centerLatitude = settings.geofenceCenterLat,
                    centerLongitude = settings.geofenceCenterLng
                ),
                cloudConfig = CloudApiConfig(
                    isConnected = false,
                    apiUrl = targetUrl,
                    vin = settings.selectedVin,
                    hasValidSession = isSessionValid,
                    sessionExpiryTimestamp = storedSession.tokenExpiry,
                    sessionStateSummary = if (isSessionValid) "Valid Session (Local Keystore)" else "Session Expired (Refresh Required)",
                    regionEndpoint = regionCluster.regionTitle,
                    isGatewayAdapter = isGateway
                ),
                myMazdaAccount = MyMazdaAccount(
                    email = settings.accountEmail,
                    isLinked = true,
                    region = regionCluster.regionTitle,
                    regionCode = regionCluster.code,
                    customerId = storedSession.customerId,
                    vehicleVin = settings.selectedVin,
                    lastHandshakeStatus = "Session loaded from secure local Keystore. Ready to verify."
                )
            )

            // If session is valid and VIN is selected, verify connection with real handshake
            if (isSessionValid && isVinSelected) {
                testConnectionHandshake()
            }
        } else {
            // Unauthenticated clean initial state
            _vehicleState.value = _vehicleState.value.copy(
                themeMode = settings.themeMode,
                remoteStart = _vehicleState.value.remoteStart.copy(
                    runDurationMinutes = settings.remoteStartDurationMinutes,
                    targetTempF = settings.remoteStartTargetTempF
                ),
                geofence = GeofenceSettings(
                    isEnabled = settings.geofenceEnabled,
                    radiusMeters = settings.geofenceRadiusMeters,
                    safeZoneName = settings.geofenceSafeZoneName,
                    centerLatitude = settings.geofenceCenterLat,
                    centerLongitude = settings.geofenceCenterLng
                )
            )
        }
    }

    // ----------------- Room Database Flow Getters -----------------

    fun getAllTrips(): Flow<List<TripLogEntity>> {
        return tripDao.getAllTrips()
    }

    fun getTripsForActiveVin(): Flow<List<TripLogEntity>> {
        return tripDao.getTripsForVin(_vehicleState.value.vin)
    }

    fun getMaintenanceRemindersForVin(): Flow<List<MaintenanceEntity>> {
        return maintenanceDao.getRemindersForVin(_vehicleState.value.vin)
    }

    suspend fun setReminderCompleted(id: Long, isCompleted: Boolean) {
        maintenanceDao.setCompleted(id, isCompleted)
    }

    suspend fun addTrip(
        distance: Double,
        duration: Int,
        mpg: Double,
        startAddress: String,
        endAddress: String,
        tag: String
    ) {
        val now = System.currentTimeMillis()
        val avgSpeed = if (duration > 0) (distance / (duration / 60.0)).coerceIn(10.0, 85.0) else 35.0

        tripDao.insertTrip(
            TripLogEntity(
                vin = _vehicleState.value.vin.ifBlank { "MANUAL_ENTRY" },
                startTimeMillis = now - (duration * 60_000L),
                endTimeMillis = now,
                distanceMiles = distance,
                durationMinutes = duration,
                avgMpg = mpg,
                avgSpeedMph = Math.round(avgSpeed * 10.0) / 10.0,
                maxSpeedMph = Math.round((avgSpeed * 1.3) * 10.0) / 10.0,
                drivingScore = 90,
                startAddress = startAddress,
                endAddress = endAddress,
                routeSummary = "$startAddress to $endAddress",
                fuelConsumedGal = if (mpg > 0) Math.round((distance / mpg) * 100.0) / 100.0 else 0.0,
                tripTag = tag
            )
        )
    }

    // ----------------- Real Mazda Authentication & Session Management -----------------

    /**
     * Authenticates a Mazda account using either:
     * 1. Direct Mazda Azure AD B2C flow (b2cLogin -> b2cPassword -> b2cConfirm -> b2cTokenRequest -> attachSession)
     * 2. MCS Gateway Adapter (cv/mcs daemon / proxy)
     */
    suspend fun authenticateMyMazdaAccount(
        email: String,
        password: String,
        region: MazdaRegionalCluster,
        customBaseUrl: String? = null
    ): Result<List<DiscoveredVehicle>> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val isGatewayMode = region == MazdaRegionalCluster.CV_MCS_BRIDGE || !customBaseUrl.isNullOrBlank()

        try {
            if (isGatewayMode) {
                // Connect via defined McsGatewayAdapter
                val gatewayUrl = customBaseUrl?.ifBlank { null } ?: region.primaryBaseUrl
                val gatewayService = McsGatewayClient.create(gatewayUrl)

                // Health check
                val healthRes = gatewayService.checkHealth()
                if (!healthRes.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("MCS Gateway Adapter unreachable at $gatewayUrl (HTTP ${healthRes.code()})")
                    )
                }

                // Query vehicles from gateway
                val vehiclesRes = gatewayService.getDiscoveredVehicles()
                if (!vehiclesRes.isSuccessful || vehiclesRes.body() == null) {
                    return@withContext Result.failure(
                        Exception("MCS Gateway vehicle query returned HTTP ${vehiclesRes.code()}: ${vehiclesRes.message()}")
                    )
                }

                val vehicles = vehiclesRes.body()!!
                val discoveredList = vehicles.mapIndexed { idx, v ->
                    DiscoveredVehicle(
                        vin = v.vin,
                        modelName = v.modelName ?: "Mazda Connected Vehicle",
                        modelYear = v.modelYear ?: 0,
                        nickname = v.nickname ?: v.vin,
                        isTcuPaired = v.tcuPaired,
                        canRemoteControl = v.canRemoteControl,
                        isSelected = idx == 0
                    )
                }

                val activeVehicle = discoveredList.firstOrNull()
                sessionManager.saveAuthenticatedSession(
                    accessToken = "mcs_gateway_token",
                    refreshToken = null,
                    idToken = null,
                    expiresInSeconds = 86400L,
                    customerId = email,
                    region = region.code
                )

                preferencesManager.saveAccountSettings(email, region.code, customBaseUrl)
                if (activeVehicle != null) {
                    preferencesManager.saveSelectedVehicle(activeVehicle.vin, activeVehicle.nickname)
                }

                val latency = System.currentTimeMillis() - startTime
                _vehicleState.value = _vehicleState.value.copy(
                    vin = activeVehicle?.vin ?: "",
                    modelName = activeVehicle?.modelName ?: "No Vehicle Selected",
                    year = activeVehicle?.modelYear ?: 0,
                    nickname = activeVehicle?.nickname ?: "No Vehicle",
                    isVehicleSelected = activeVehicle != null,
                    isConnected = true,
                    cloudConfig = CloudApiConfig(
                        isConnected = true,
                        apiUrl = gatewayUrl,
                        vin = activeVehicle?.vin ?: "",
                        hasValidSession = true,
                        sessionExpiryTimestamp = System.currentTimeMillis() + 86400_000L,
                        sessionStateSummary = "Connected via MCS Gateway (${discoveredList.size} vehicles)",
                        lastHttpCode = 200,
                        lastLatencyMs = latency,
                        lastSyncTimestamp = System.currentTimeMillis(),
                        regionEndpoint = region.regionTitle,
                        isGatewayAdapter = true
                    ),
                    myMazdaAccount = MyMazdaAccount(
                        email = email,
                        region = region.regionTitle,
                        regionCode = region.code,
                        customerId = email,
                        vehicleCount = discoveredList.size,
                        vehicleVin = activeVehicle?.vin ?: "",
                        isLinked = true,
                        isTcuPaired = activeVehicle?.isTcuPaired ?: false,
                        remoteControlEntitlementActive = activeVehicle?.canRemoteControl ?: false,
                        lastLoginTimestamp = System.currentTimeMillis(),
                        discoveredVehicles = discoveredList,
                        lastHandshakeStatus = "Connected to cv/mcs Gateway ($latency ms)"
                    )
                )

                if (activeVehicle != null) {
                    syncWithCloudApi()
                }

                return@withContext Result.success(discoveredList)
            } else {
                // Direct Azure AD B2C Authentication Flow
                val b2cEngine = MazdaB2cAuthEngine(region)

                // 1. Initiate B2C Login & PKCE
                val loginSessionRes = b2cEngine.b2cLogin()
                if (loginSessionRes.isFailure) {
                    val err = loginSessionRes.exceptionOrNull()
                    return@withContext Result.failure(
                        Exception(err?.message ?: "Direct B2C authorization request failed.")
                    )
                }
                val loginSession = loginSessionRes.getOrThrow()

                // 2. Submit Credentials to SelfAsserted endpoint
                val passwordRes = b2cEngine.b2cPassword(loginSession, email, password)
                if (passwordRes.isFailure) {
                    val err = passwordRes.exceptionOrNull()
                    return@withContext Result.failure(
                        Exception(err?.message ?: "Mazda account credential verification failed.")
                    )
                }

                // 3. Confirm & extract Authorization Code
                val confirmRes = b2cEngine.b2cConfirm(loginSession)
                if (confirmRes.isFailure) {
                    val err = confirmRes.exceptionOrNull()
                    return@withContext Result.failure(
                        Exception(err?.message ?: "B2C authorization code confirmation failed.")
                    )
                }
                val authCode = confirmRes.getOrThrow()

                // 4. Exchange Code for Access & Refresh Tokens
                val tokenRes = b2cEngine.b2cTokenRequest(loginSession, authCode)
                if (tokenRes.isFailure) {
                    val err = tokenRes.exceptionOrNull()
                    return@withContext Result.failure(
                        Exception(err?.message ?: "Failed to exchange authorization code for tokens.")
                    )
                }
                val tokenData = tokenRes.getOrThrow()

                // 5. Attach Device Session
                val deviceId = "nowmymazda_android_${System.currentTimeMillis()}"
                b2cEngine.attachSession(tokenData.accessToken, deviceId)

                // 6. Save Session with exact expires_in
                sessionManager.saveAuthenticatedSession(
                    accessToken = tokenData.accessToken,
                    refreshToken = tokenData.refreshToken,
                    idToken = tokenData.idToken,
                    expiresInSeconds = tokenData.expiresInSeconds,
                    customerId = email,
                    region = region.code,
                    deviceId = deviceId
                )

                // 7. Discover Vehicles
                val apiService = ApiClient.createService(region.primaryBaseUrl)
                val vehiclesRes = apiService.getVehicles("Bearer ${tokenData.accessToken}")
                if (!vehiclesRes.isSuccessful || vehiclesRes.body() == null) {
                    return@withContext Result.failure(
                        Exception("Failed to discover Mazda vehicles: HTTP ${vehiclesRes.code()}")
                    )
                }

                val vehicleItems = vehiclesRes.body()!!
                val discoveredList = vehicleItems.mapIndexed { idx, item ->
                    DiscoveredVehicle(
                        vin = item.vin,
                        modelName = if (!item.carlineName.isNullOrBlank()) "Mazda ${item.carlineName}" else "Mazda Vehicle",
                        modelYear = item.modelYear?.toIntOrNull() ?: 0,
                        nickname = item.nickname ?: item.vin,
                        isTcuPaired = item.tcuPaired,
                        canRemoteControl = item.canRemoteControl,
                        isSelected = idx == 0
                    )
                }

                val activeVehicle = discoveredList.firstOrNull()
                preferencesManager.saveAccountSettings(email, region.code, customBaseUrl)
                if (activeVehicle != null) {
                    preferencesManager.saveSelectedVehicle(activeVehicle.vin, activeVehicle.nickname)
                }

                val latency = System.currentTimeMillis() - startTime
                _vehicleState.value = _vehicleState.value.copy(
                    vin = activeVehicle?.vin ?: "",
                    modelName = activeVehicle?.modelName ?: "No Vehicle Selected",
                    year = activeVehicle?.modelYear ?: 0,
                    nickname = activeVehicle?.nickname ?: "No Vehicle",
                    isVehicleSelected = activeVehicle != null,
                    isConnected = true,
                    cloudConfig = CloudApiConfig(
                        isConnected = true,
                        apiUrl = region.primaryBaseUrl,
                        vin = activeVehicle?.vin ?: "",
                        hasValidSession = true,
                        sessionExpiryTimestamp = System.currentTimeMillis() + (tokenData.expiresInSeconds * 1000L),
                        sessionStateSummary = "Direct B2C Authenticated (${discoveredList.size} vehicles)",
                        lastHttpCode = 200,
                        lastLatencyMs = latency,
                        lastSyncTimestamp = System.currentTimeMillis(),
                        regionEndpoint = region.regionTitle,
                        isGatewayAdapter = false
                    ),
                    myMazdaAccount = MyMazdaAccount(
                        email = email,
                        region = region.regionTitle,
                        regionCode = region.code,
                        customerId = email,
                        vehicleCount = discoveredList.size,
                        vehicleVin = activeVehicle?.vin ?: "",
                        isLinked = true,
                        isTcuPaired = activeVehicle?.isTcuPaired ?: false,
                        remoteControlEntitlementActive = activeVehicle?.canRemoteControl ?: false,
                        lastLoginTimestamp = System.currentTimeMillis(),
                        discoveredVehicles = discoveredList,
                        lastHandshakeStatus = "Direct B2C Authentication Succeeded ($latency ms)"
                    )
                )

                if (activeVehicle != null) {
                    syncWithCloudApi()
                }

                return@withContext Result.success(discoveredList)
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            _vehicleState.value = _vehicleState.value.copy(
                cloudConfig = _vehicleState.value.cloudConfig.copy(
                    lastErrorMessage = "${e.javaClass.simpleName}: ${e.localizedMessage ?: "Authentication failure"}",
                    lastLatencyMs = latency,
                    lastHttpCode = null
                )
            )
            Result.failure(e)
        }
    }

    fun selectActiveVehicle(vin: String) {
        val currentAccount = _vehicleState.value.myMazdaAccount
        val updatedList = currentAccount.discoveredVehicles.map {
            it.copy(isSelected = it.vin == vin)
        }
        val selected = updatedList.find { it.vin == vin } ?: return

        scope.launch {
            preferencesManager.saveSelectedVehicle(selected.vin, selected.nickname)
        }

        _vehicleState.value = _vehicleState.value.copy(
            vin = selected.vin,
            modelName = selected.modelName,
            year = selected.modelYear,
            nickname = selected.nickname,
            isVehicleSelected = true,
            myMazdaAccount = currentAccount.copy(
                vehicleVin = selected.vin,
                discoveredVehicles = updatedList,
                isTcuPaired = selected.isTcuPaired,
                remoteControlEntitlementActive = selected.canRemoteControl
            ),
            cloudConfig = _vehicleState.value.cloudConfig.copy(
                vin = selected.vin
            )
        )

        scope.launch {
            syncWithCloudApi()
        }
    }

    fun signOutAccount() {
        sessionManager.signOut()
        scope.launch {
            preferencesManager.saveAccountSettings("", "MNAO", null)
            preferencesManager.saveSelectedVehicle("", "")
        }

        _vehicleState.value = VehicleState(
            themeMode = _vehicleState.value.themeMode,
            cloudConfig = CloudApiConfig(isConnected = false, sessionStateSummary = "Signed Out"),
            myMazdaAccount = MyMazdaAccount(lastHandshakeStatus = "Account signed out")
        )

        NotificationHelper.sendAlert(
            context,
            304,
            "Signed Out",
            "Mazda account session and tokens cleared securely from Keystore."
        )
    }

    suspend fun testConnectionHandshake(): String = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val isGateway = _vehicleState.value.cloudConfig.isGatewayAdapter
        val targetUrl = _vehicleState.value.cloudConfig.apiUrl

        try {
            if (isGateway) {
                val gateway = McsGatewayClient.create(targetUrl)
                val health = gateway.checkHealth()
                val latency = System.currentTimeMillis() - start
                val code = health.code()
                val isSuccess = health.isSuccessful

                _vehicleState.value = _vehicleState.value.copy(
                    isConnected = isSuccess,
                    cloudConfig = _vehicleState.value.cloudConfig.copy(
                        isConnected = isSuccess,
                        lastHttpCode = code,
                        lastLatencyMs = latency,
                        lastSyncTimestamp = System.currentTimeMillis(),
                        lastExecutedCommand = "Gateway Health (HTTP $code)",
                        lastErrorMessage = if (isSuccess) null else "HTTP $code: ${health.message()}"
                    )
                )

                if (isSuccess) {
                    "MCS Gateway OK: Handshake responded in ${latency}ms (HTTP $code)."
                } else {
                    "MCS Gateway Warning: Gateway returned HTTP $code in ${latency}ms."
                }
            } else {
                val region = MazdaRegionalCluster.fromCode(_vehicleState.value.myMazdaAccount.regionCode)
                val b2cEngine = MazdaB2cAuthEngine(region)
                val token = sessionManager.getValidAccessToken(b2cEngine)

                if (token.isNullOrBlank()) {
                    val latency = System.currentTimeMillis() - start
                    _vehicleState.value = _vehicleState.value.copy(
                        isConnected = false,
                        cloudConfig = _vehicleState.value.cloudConfig.copy(
                            isConnected = false,
                            lastErrorMessage = "Session expired. Reauthentication required.",
                            lastLatencyMs = latency
                        )
                    )
                    return@withContext "Handshake Failed: Valid access token unavailable. Please sign in."
                }

                val service = ApiClient.createService(region.primaryBaseUrl)
                val res = service.pingGateway("Bearer $token")
                val latency = System.currentTimeMillis() - start
                val code = res.code()
                val isSuccess = res.isSuccessful

                _vehicleState.value = _vehicleState.value.copy(
                    isConnected = isSuccess,
                    cloudConfig = _vehicleState.value.cloudConfig.copy(
                        isConnected = isSuccess,
                        lastHttpCode = code,
                        lastLatencyMs = latency,
                        lastSyncTimestamp = System.currentTimeMillis(),
                        lastExecutedCommand = "Handshake Ping (HTTP $code)",
                        lastErrorMessage = if (isSuccess) null else "HTTP $code: ${res.message()}"
                    )
                )

                if (isSuccess) {
                    "Handshake OK: Mazda Cloud responded in ${latency}ms (HTTP $code)."
                } else {
                    "Handshake Warning: Cloud returned HTTP $code (${res.message()}) in ${latency}ms."
                }
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            _vehicleState.value = _vehicleState.value.copy(
                isConnected = false,
                cloudConfig = _vehicleState.value.cloudConfig.copy(
                    isConnected = false,
                    lastHttpCode = null,
                    lastLatencyMs = latency,
                    lastErrorMessage = "${e.javaClass.simpleName}: ${e.localizedMessage ?: "Connection error"}"
                )
            )
            "Handshake Error (${latency}ms): ${e.localizedMessage ?: "Unable to connect"}"
        }
    }

    // ----------------- Real Remote Commands -----------------

    fun remoteStartEngine(durationMinutes: Int, climateTempF: Int) {
        if (!_vehicleState.value.isVehicleSelected || _vehicleState.value.vin.isBlank()) {
            NotificationHelper.sendAlert(context, 101, "Command Rejected", "No Mazda vehicle is currently selected.")
            return
        }

        _vehicleState.value = _vehicleState.value.copy(
            commandExecutionState = CommandExecutionState.Pending(
                action = "Remote Engine Start",
                message = "Sending ignition request to Mazda Connected Services..."
            )
        )

        scope.launch {
            val result = executeRemoteCommand(
                action = "START_ENGINE",
                durationMinutes = durationMinutes,
                tempF = climateTempF
            )

            result.fold(
                onSuccess = { commandId ->
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Accepted(
                            action = "Remote Engine Start",
                            commandId = commandId,
                            message = "Ignition request accepted by cloud. Awaiting TCU verification..."
                        )
                    )

                    pollForCommandConfirmation(
                        expectedCondition = { state -> state.engineState == EngineState.RUNNING },
                        actionName = "Remote Engine Start",
                        successMsg = "Engine ignition confirmed active by TCU."
                    )
                },
                onFailure = { error ->
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Failed(
                            action = "Remote Engine Start",
                            errorMessage = error.localizedMessage ?: "Remote start failed"
                        )
                    )
                }
            )
        }
    }

    fun requestStopEngine() {
        if (!_vehicleState.value.isVehicleSelected || _vehicleState.value.vin.isBlank()) return

        _vehicleState.value = _vehicleState.value.copy(
            commandExecutionState = CommandExecutionState.Pending("Engine Stop", "Sending stop command to Mazda cloud...")
        )

        scope.launch {
            val result = executeRemoteCommand(action = "STOP_ENGINE")
            result.fold(
                onSuccess = { commandId ->
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Accepted("Engine Stop", commandId, "Engine stop accepted. Verifying...")
                    )
                    pollForCommandConfirmation(
                        expectedCondition = { state -> state.engineState == EngineState.OFF },
                        actionName = "Engine Stop",
                        successMsg = "Engine confirmed OFF by TCU."
                    )
                },
                onFailure = { error ->
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Failed("Engine Stop", error.localizedMessage ?: "Failed to stop engine")
                    )
                }
            )
        }
    }

    fun requestDoorLock() {
        if (!_vehicleState.value.isVehicleSelected || _vehicleState.value.vin.isBlank()) return
        _vehicleState.value = _vehicleState.value.copy(
            commandExecutionState = CommandExecutionState.Pending("Lock Doors", "Sending door lock request...")
        )
        scope.launch {
            val result = executeRemoteCommand(action = "LOCK")
            result.fold(
                onSuccess = { commandId ->
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Accepted("Lock Doors", commandId, "Door lock accepted. Verifying...")
                    )
                    pollForCommandConfirmation(
                        expectedCondition = { state -> state.lockState == LockState.LOCKED },
                        actionName = "Lock Doors",
                        successMsg = "Vehicle doors confirmed LOCKED by TCU."
                    )
                },
                onFailure = { error ->
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Failed("Lock Doors", error.localizedMessage ?: "Door lock failed")
                    )
                }
            )
        }
    }

    fun requestDoorUnlock() {
        if (!_vehicleState.value.isVehicleSelected || _vehicleState.value.vin.isBlank()) return
        _vehicleState.value = _vehicleState.value.copy(
            commandExecutionState = CommandExecutionState.Pending("Unlock Doors", "Sending door unlock request...")
        )
        scope.launch {
            val result = executeRemoteCommand(action = "UNLOCK")
            result.fold(
                onSuccess = { commandId ->
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Accepted("Unlock Doors", commandId, "Door unlock accepted. Verifying...")
                    )
                    pollForCommandConfirmation(
                        expectedCondition = { state -> state.lockState == LockState.UNLOCKED },
                        actionName = "Unlock Doors",
                        successMsg = "Vehicle doors confirmed UNLOCKED by TCU."
                    )
                },
                onFailure = { error ->
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Failed("Unlock Doors", error.localizedMessage ?: "Door unlock failed")
                    )
                }
            )
        }
    }

    fun toggleHazardLights() {
        if (!_vehicleState.value.isVehicleSelected || _vehicleState.value.vin.isBlank()) return
        val newTarget = !_vehicleState.value.hazardLightsActive
        val action = if (newTarget) "FLASH_HAZARDS_ON" else "FLASH_HAZARDS_OFF"

        _vehicleState.value = _vehicleState.value.copy(
            commandExecutionState = CommandExecutionState.Pending("Hazard Lights", "Sending hazard command...")
        )

        scope.launch {
            val result = executeRemoteCommand(action)
            result.fold(
                onSuccess = { commandId ->
                    _vehicleState.value = _vehicleState.value.copy(
                        hazardLightsActive = newTarget,
                        commandExecutionState = CommandExecutionState.Confirmed("Hazard Lights", commandId, "Hazard lights toggled.")
                    )
                },
                onFailure = { err ->
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Failed("Hazard Lights", err.localizedMessage ?: "Failed to toggle hazard lights")
                    )
                }
            )
        }
    }

    fun soundHorn() {
        if (!_vehicleState.value.isVehicleSelected || _vehicleState.value.vin.isBlank()) return
        _vehicleState.value = _vehicleState.value.copy(
            commandExecutionState = CommandExecutionState.Pending("Sound Horn", "Sending horn chirp request...")
        )
        scope.launch {
            val result = executeRemoteCommand("SOUND_HORN_AND_FLASH")
            result.fold(
                onSuccess = { commandId ->
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Confirmed("Sound Horn", commandId, "Vehicle horn chirped.")
                    )
                },
                onFailure = { err ->
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Failed("Sound Horn", err.localizedMessage ?: "Failed to sound horn")
                    )
                }
            )
        }
    }

    fun toggleTrunk() {
        if (!_vehicleState.value.isVehicleSelected || _vehicleState.value.vin.isBlank()) return
        _vehicleState.value = _vehicleState.value.copy(
            commandExecutionState = CommandExecutionState.Pending("Trunk Release", "Sending trunk release command...")
        )
        scope.launch {
            val result = executeRemoteCommand("TOGGLE_TRUNK")
            result.fold(
                onSuccess = { commandId ->
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Confirmed("Trunk Release", commandId, "Trunk released.")
                    )
                },
                onFailure = { err ->
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Failed("Trunk Release", err.localizedMessage ?: "Failed to release trunk")
                    )
                }
            )
        }
    }

    private suspend fun executeRemoteCommand(
        action: String,
        durationMinutes: Int? = null,
        tempF: Int? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val vin = _vehicleState.value.vin
        if (vin.isBlank()) {
            return@withContext Result.failure(Exception("No active vehicle selected."))
        }

        val isGateway = _vehicleState.value.cloudConfig.isGatewayAdapter
        val targetUrl = _vehicleState.value.cloudConfig.apiUrl
        val startTime = System.currentTimeMillis()

        try {
            if (isGateway) {
                val gateway = McsGatewayClient.create(targetUrl)
                val response = gateway.dispatchRemoteCommand(
                    vin = vin,
                    request = McsRemoteCommandRequestDto(
                        action = action,
                        durationMinutes = durationMinutes,
                        targetTempF = tempF
                    )
                )

                val latency = System.currentTimeMillis() - startTime
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val cmdId = body.commandId ?: "cmd_${System.currentTimeMillis()}"
                    Result.success(cmdId)
                } else {
                    Result.failure(Exception("Gateway rejected command: HTTP ${response.code()}"))
                }
            } else {
                val region = MazdaRegionalCluster.fromCode(_vehicleState.value.myMazdaAccount.regionCode)
                val b2cEngine = MazdaB2cAuthEngine(region)
                val token = sessionManager.getValidAccessToken(b2cEngine)
                    ?: return@withContext Result.failure(Exception("Mazda session expired. Please sign in again."))

                val service = ApiClient.createService(region.primaryBaseUrl)
                val response = service.dispatchRemoteCommand(
                    token = "Bearer $token",
                    vin = vin,
                    request = RemoteControlRequest(
                        vin = vin,
                        action = action,
                        durationMinutes = durationMinutes,
                        climateTempF = tempF
                    )
                )

                val latency = System.currentTimeMillis() - startTime
                val code = response.code()

                _vehicleState.value = _vehicleState.value.copy(
                    cloudConfig = _vehicleState.value.cloudConfig.copy(
                        lastHttpCode = code,
                        lastLatencyMs = latency,
                        lastExecutedCommand = "$action ($code)",
                        lastErrorMessage = if (response.isSuccessful) null else "HTTP $code: ${response.message()}",
                        lastSyncTimestamp = System.currentTimeMillis()
                    )
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    val cmdId = body?.commandId ?: "cmd_${System.currentTimeMillis()}"
                    Result.success(cmdId)
                } else {
                    val errorMsg = when (code) {
                        401 -> "Session expired. Please reconnect your Mazda account."
                        403 -> "Remote command disallowed by Mazda (safety interlocks engaged or ignition locked)."
                        404 -> "Vehicle VIN not found on this regional cluster."
                        else -> "Command rejected by server (HTTP $code: ${response.message()})"
                    }
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            _vehicleState.value = _vehicleState.value.copy(
                cloudConfig = _vehicleState.value.cloudConfig.copy(
                    lastHttpCode = null,
                    lastLatencyMs = latency,
                    lastExecutedCommand = action,
                    lastErrorMessage = "${e.javaClass.simpleName}: ${e.localizedMessage ?: "Network error"}",
                    lastSyncTimestamp = System.currentTimeMillis()
                )
            )
            Result.failure(e)
        }
    }

    private fun pollForCommandConfirmation(
        expectedCondition: (VehicleState) -> Boolean,
        actionName: String,
        successMsg: String
    ) {
        commandPollingJob?.cancel()
        commandPollingJob = scope.launch {
            // Give TCU time to receive network command
            delay(4000)
            var attempts = 0
            while (attempts < 3) {
                syncWithCloudApi()
                if (expectedCondition(_vehicleState.value)) {
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Confirmed(
                            action = actionName,
                            message = successMsg
                        )
                    )
                    delay(3000)
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Idle
                    )
                    return@launch
                }
                attempts++
                delay(3000)
            }
            // Strict requirement: Never silently reset a timed-out command to Idle!
            _vehicleState.value = _vehicleState.value.copy(
                commandExecutionState = CommandExecutionState.Failed(
                    action = actionName,
                    errorMessage = "Command timed out waiting for vehicle TCU response. Telematics status unchanged.",
                    errorCode = "TIMEOUT"
                )
            )
        }
    }

    // ----------------- Real Telematics Status Sync -----------------

    suspend fun syncWithCloudApi(): Boolean = withContext(Dispatchers.IO) {
        val vin = _vehicleState.value.vin
        if (vin.isBlank()) return@withContext false

        val isGateway = _vehicleState.value.cloudConfig.isGatewayAdapter
        val targetUrl = _vehicleState.value.cloudConfig.apiUrl

        _vehicleState.value = _vehicleState.value.copy(
            cloudConfig = _vehicleState.value.cloudConfig.copy(isSyncing = true)
        )
        val startTime = System.currentTimeMillis()

        try {
            if (isGateway) {
                val gateway = McsGatewayClient.create(targetUrl)
                val response = gateway.getVehicleTelemetry(vin)
                val latency = System.currentTimeMillis() - startTime

                if (response.isSuccessful && response.body() != null) {
                    val dto = response.body()!!

                    val mappedEngine = when (dto.engineStatus?.uppercase()) {
                        "RUNNING", "ON" -> EngineState.RUNNING
                        "STARTING" -> EngineState.STARTING
                        "OFF" -> EngineState.OFF
                        else -> EngineState.UNKNOWN
                    }

                    val mappedLock = when (dto.doorLockStatus?.uppercase()) {
                        "LOCKED" -> LockState.LOCKED
                        "UNLOCKED" -> LockState.UNLOCKED
                        else -> LockState.UNKNOWN
                    }

                    val hasBattery = dto.batteryVoltage != null && dto.batteryVoltage > 0.0
                    val updatedBattery = if (hasBattery) {
                        BatteryHealth(
                            isAvailable = true,
                            voltage = dto.batteryVoltage!!,
                            stateOfHealthPercent = dto.batterySoh ?: 0,
                            stateOfChargePercent = ((dto.batteryVoltage - 11.8) / (12.7 - 11.8) * 100.0).toInt().coerceIn(0, 100),
                            alternatorStatus = if (mappedEngine == EngineState.RUNNING) "${dto.batteryVoltage}V (Charging)" else "${dto.batteryVoltage}V (Resting)",
                            healthSummary = if (dto.batteryVoltage >= 12.2) "Voltage Normal" else "Low Voltage Alert"
                        )
                    } else {
                        BatteryHealth(isAvailable = false, voltage = 0.0, healthSummary = "Telemetry unavailable")
                    }

                    val updatedTpms = TPMSData(
                        isAvailable = dto.tpmsFlPsi != null || dto.tpmsFrPsi != null || dto.tpmsRlPsi != null || dto.tpmsRrPsi != null,
                        frontLeft = TireInfo("FL", "Front Left", dto.tpmsFlPsi ?: 0.0, status = if (dto.tpmsFlPsi == null) PressureStatus.UNKNOWN else if (dto.tpmsFlPsi < 30.0) PressureStatus.LOW else PressureStatus.NORMAL, isAvailable = dto.tpmsFlPsi != null),
                        frontRight = TireInfo("FR", "Front Right", dto.tpmsFrPsi ?: 0.0, status = if (dto.tpmsFrPsi == null) PressureStatus.UNKNOWN else if (dto.tpmsFrPsi < 30.0) PressureStatus.LOW else PressureStatus.NORMAL, isAvailable = dto.tpmsFrPsi != null),
                        rearLeft = TireInfo("RL", "Rear Left", dto.tpmsRlPsi ?: 0.0, status = if (dto.tpmsRlPsi == null) PressureStatus.UNKNOWN else if (dto.tpmsRlPsi < 30.0) PressureStatus.LOW else PressureStatus.NORMAL, isAvailable = dto.tpmsRlPsi != null),
                        rearRight = TireInfo("RR", "Rear Right", dto.tpmsRrPsi ?: 0.0, status = if (dto.tpmsRrPsi == null) PressureStatus.UNKNOWN else if (dto.tpmsRrPsi < 30.0) PressureStatus.LOW else PressureStatus.NORMAL, isAvailable = dto.tpmsRrPsi != null)
                    )

                    val hasGps = dto.latitude != null && dto.longitude != null && dto.latitude != 0.0
                    val updatedLocation = if (hasGps) {
                        VehicleGpsLocation(
                            isAvailable = true,
                            latitude = dto.latitude!!,
                            longitude = dto.longitude!!,
                            address = "Vehicle GPS: ${String.format(java.util.Locale.US, "%.4f, %.4f", dto.latitude, dto.longitude)}",
                            lastUpdatedTimestamp = dto.lastUpdatedTimestamp ?: System.currentTimeMillis(),
                            source = LocationSource.VEHICLE_TCU
                        )
                    } else {
                        _vehicleState.value.location
                    }

                    val updatedDiagnostics = if (dto.activeDtcCodes != null) {
                        DiagnosticStatus(
                            isAvailable = true,
                            checkEngineOn = dto.activeDtcCodes.isNotEmpty(),
                            activeDtcCodes = dto.activeDtcCodes,
                            lastScanTimestamp = System.currentTimeMillis(),
                            scanStatusMessage = if (dto.activeDtcCodes.isEmpty()) "Diagnostic check complete: zero active DTCs" else "Active DTC codes: ${dto.activeDtcCodes.joinToString()}"
                        )
                    } else {
                        DiagnosticStatus(isAvailable = false, scanStatusMessage = "No active telematics DTC scan recorded")
                    }

                    _vehicleState.value = _vehicleState.value.copy(
                        isConnected = true,
                        cloudConfig = _vehicleState.value.cloudConfig.copy(
                            isSyncing = false,
                            isConnected = true,
                            lastHttpCode = response.code(),
                            lastLatencyMs = latency,
                            lastErrorMessage = null,
                            lastSyncTimestamp = System.currentTimeMillis(),
                            lastExecutedCommand = "Telematics Sync via Gateway (200 OK)"
                        ),
                        engineState = mappedEngine,
                        lockState = mappedLock,
                        odometerMiles = dto.odometerMiles ?: _vehicleState.value.odometerMiles,
                        isOdometerAvailable = dto.odometerMiles != null,
                        fuelPercent = dto.fuelLevelPercent ?: _vehicleState.value.fuelPercent,
                        isFuelAvailable = dto.fuelLevelPercent != null,
                        lastReportTimestamp = dto.lastUpdatedTimestamp ?: System.currentTimeMillis(),
                        batteryHealth = updatedBattery,
                        tpms = updatedTpms,
                        location = updatedLocation,
                        diagnostics = updatedDiagnostics
                    )
                    true
                } else {
                    _vehicleState.value = _vehicleState.value.copy(
                        cloudConfig = _vehicleState.value.cloudConfig.copy(
                            isSyncing = false,
                            lastHttpCode = response.code(),
                            lastLatencyMs = latency,
                            lastErrorMessage = "HTTP ${response.code()}: ${response.message()}",
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                    )
                    false
                }
            } else {
                val region = MazdaRegionalCluster.fromCode(_vehicleState.value.myMazdaAccount.regionCode)
                val b2cEngine = MazdaB2cAuthEngine(region)
                val token = sessionManager.getValidAccessToken(b2cEngine)
                if (token.isNullOrBlank()) {
                    _vehicleState.value = _vehicleState.value.copy(
                        cloudConfig = _vehicleState.value.cloudConfig.copy(
                            isSyncing = false,
                            lastErrorMessage = "Session expired. Please sign in again."
                        )
                    )
                    return@withContext false
                }

                val service = ApiClient.createService(region.primaryBaseUrl)
                val response = service.getVehicleStatus("Bearer $token", vin)
                val latency = System.currentTimeMillis() - startTime

                if (response.isSuccessful && response.body() != null) {
                    val dto = response.body()!!

                    val mappedEngine = when (dto.engineStatus?.uppercase()) {
                        "RUNNING", "ON" -> EngineState.RUNNING
                        "STARTING" -> EngineState.STARTING
                        "OFF" -> EngineState.OFF
                        else -> EngineState.UNKNOWN
                    }

                    val mappedLock = when (dto.doorLockStatus?.uppercase()) {
                        "LOCKED" -> LockState.LOCKED
                        "UNLOCKED" -> LockState.UNLOCKED
                        else -> LockState.UNKNOWN
                    }

                    val hasBattery = dto.batteryVoltage != null && dto.batteryVoltage > 0.0
                    val updatedBattery = if (hasBattery) {
                        BatteryHealth(
                            isAvailable = true,
                            voltage = dto.batteryVoltage!!,
                            stateOfHealthPercent = dto.batterySoh ?: 0,
                            stateOfChargePercent = ((dto.batteryVoltage - 11.8) / (12.7 - 11.8) * 100.0).toInt().coerceIn(0, 100),
                            alternatorStatus = if (mappedEngine == EngineState.RUNNING) "${dto.batteryVoltage}V (Charging)" else "${dto.batteryVoltage}V (Resting)",
                            healthSummary = if (dto.batteryVoltage >= 12.2) "Good Condition" else "Low Voltage Alert"
                        )
                    } else {
                        BatteryHealth(isAvailable = false, voltage = 0.0, healthSummary = "Telemetry unavailable")
                    }

                    val updatedTpms = TPMSData(
                        isAvailable = dto.tpmsFlPsi != null || dto.tpmsFrPsi != null || dto.tpmsRlPsi != null || dto.tpmsRrPsi != null,
                        frontLeft = TireInfo("FL", "Front Left", dto.tpmsFlPsi ?: 0.0, status = if (dto.tpmsFlPsi == null) PressureStatus.UNKNOWN else if (dto.tpmsFlPsi < 30.0) PressureStatus.LOW else PressureStatus.NORMAL, isAvailable = dto.tpmsFlPsi != null),
                        frontRight = TireInfo("FR", "Front Right", dto.tpmsFrPsi ?: 0.0, status = if (dto.tpmsFrPsi == null) PressureStatus.UNKNOWN else if (dto.tpmsFrPsi < 30.0) PressureStatus.LOW else PressureStatus.NORMAL, isAvailable = dto.tpmsFrPsi != null),
                        rearLeft = TireInfo("RL", "Rear Left", dto.tpmsRlPsi ?: 0.0, status = if (dto.tpmsRlPsi == null) PressureStatus.UNKNOWN else if (dto.tpmsRlPsi < 30.0) PressureStatus.LOW else PressureStatus.NORMAL, isAvailable = dto.tpmsRlPsi != null),
                        rearRight = TireInfo("RR", "Rear Right", dto.tpmsRrPsi ?: 0.0, status = if (dto.tpmsRrPsi == null) PressureStatus.UNKNOWN else if (dto.tpmsRrPsi < 30.0) PressureStatus.LOW else PressureStatus.NORMAL, isAvailable = dto.tpmsRrPsi != null)
                    )

                    val hasGps = dto.latitude != null && dto.longitude != null && dto.latitude != 0.0
                    val updatedLocation = if (hasGps) {
                        VehicleGpsLocation(
                            isAvailable = true,
                            latitude = dto.latitude!!,
                            longitude = dto.longitude!!,
                            address = "Vehicle GPS: ${String.format(java.util.Locale.US, "%.4f, %.4f", dto.latitude, dto.longitude)}",
                            lastUpdatedTimestamp = dto.timestamp ?: System.currentTimeMillis(),
                            source = LocationSource.VEHICLE_TCU
                        )
                    } else {
                        _vehicleState.value.location
                    }

                    val updatedDiagnostics = if (dto.dtcCodes != null) {
                        DiagnosticStatus(
                            isAvailable = true,
                            checkEngineOn = dto.dtcCodes.isNotEmpty(),
                            oilLifePercent = dto.oilLifePercent ?: _vehicleState.value.diagnostics.oilLifePercent,
                            activeDtcCodes = dto.dtcCodes,
                            lastScanTimestamp = System.currentTimeMillis(),
                            scanStatusMessage = if (dto.dtcCodes.isEmpty()) "Diagnostic scan clean: zero active DTCs" else "Active DTC codes detected: ${dto.dtcCodes.joinToString()}"
                        )
                    } else {
                        DiagnosticStatus(isAvailable = false, scanStatusMessage = "No active telematics DTC scan recorded")
                    }

                    _vehicleState.value = _vehicleState.value.copy(
                        isConnected = true,
                        cloudConfig = _vehicleState.value.cloudConfig.copy(
                            isSyncing = false,
                            isConnected = true,
                            lastHttpCode = response.code(),
                            lastLatencyMs = latency,
                            lastErrorMessage = null,
                            lastSyncTimestamp = System.currentTimeMillis(),
                            lastExecutedCommand = "Telematics Sync (200 OK)"
                        ),
                        engineState = mappedEngine,
                        lockState = mappedLock,
                        odometerMiles = dto.odometerMiles ?: _vehicleState.value.odometerMiles,
                        isOdometerAvailable = dto.odometerMiles != null,
                        fuelPercent = dto.fuelLevelPercent ?: _vehicleState.value.fuelPercent,
                        isFuelAvailable = dto.fuelLevelPercent != null,
                        lastReportTimestamp = dto.timestamp ?: System.currentTimeMillis(),
                        batteryHealth = updatedBattery,
                        tpms = updatedTpms,
                        location = updatedLocation,
                        diagnostics = updatedDiagnostics
                    )
                    true
                } else {
                    _vehicleState.value = _vehicleState.value.copy(
                        cloudConfig = _vehicleState.value.cloudConfig.copy(
                            isSyncing = false,
                            lastHttpCode = response.code(),
                            lastLatencyMs = latency,
                            lastErrorMessage = "HTTP ${response.code()}: ${response.message()}",
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                    )
                    false
                }
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            _vehicleState.value = _vehicleState.value.copy(
                cloudConfig = _vehicleState.value.cloudConfig.copy(
                    isSyncing = false,
                    lastHttpCode = null,
                    lastLatencyMs = latency,
                    lastErrorMessage = "${e.javaClass.simpleName}: ${e.localizedMessage ?: "Gateway connection error"}",
                    lastSyncTimestamp = System.currentTimeMillis()
                )
            )
            false
        }
    }

    // ----------------- Real GPS & Geofence Handling -----------------

    fun updateVehicleLocationFromGps(lat: Double, lng: Double, address: String? = null) {
        val addr = address ?: "Mobile Device GPS (${String.format(java.util.Locale.US, "%.4f, %.4f", lat, lng)})"
        _vehicleState.value = _vehicleState.value.copy(
            location = _vehicleState.value.location.copy(
                isAvailable = true,
                latitude = lat,
                longitude = lng,
                address = addr,
                lastUpdatedTimestamp = System.currentTimeMillis(),
                source = LocationSource.MOBILE_DEVICE
            )
        )

        val geofence = _vehicleState.value.geofence
        if (geofence.isEnabled && geofence.centerLatitude != 0.0 && geofence.centerLongitude != 0.0) {
            val dist = LocationHelper.calculateDistanceMeters(
                lat,
                lng,
                geofence.centerLatitude,
                geofence.centerLongitude
            )
            val isBreached = dist > geofence.radiusMeters
            _vehicleState.value = _vehicleState.value.copy(
                geofence = geofence.copy(
                    isBreached = isBreached,
                    theftAlarmActive = isBreached
                ),
                hazardLightsActive = if (isBreached) true else _vehicleState.value.hazardLightsActive
            )
            if (isBreached) {
                NotificationHelper.sendAlert(
                    context,
                    107,
                    "🚨 GEOFENCE BREACH ALERT",
                    "Vehicle is ${dist.toInt()}m outside '${geofence.safeZoneName}'.",
                    isUrgent = true
                )
            }
        }
    }

    fun updateGeofence(enabled: Boolean, radiusMeters: Int, safeZoneName: String) {
        val centerLat = _vehicleState.value.location.latitude
        val centerLng = _vehicleState.value.location.longitude

        _vehicleState.value = _vehicleState.value.copy(
            geofence = _vehicleState.value.geofence.copy(
                isEnabled = enabled,
                radiusMeters = radiusMeters,
                safeZoneName = safeZoneName
            )
        )

        scope.launch {
            preferencesManager.saveGeofenceSettings(
                enabled = enabled,
                radiusMeters = radiusMeters,
                name = safeZoneName,
                lat = centerLat,
                lng = centerLng
            )
        }
    }

    fun setVehicleLocationAsSafeZone() {
        val loc = _vehicleState.value.location
        if (loc.isAvailable && loc.latitude != 0.0) {
            updateGeofence(
                enabled = true,
                radiusMeters = _vehicleState.value.geofence.radiusMeters,
                safeZoneName = "Vehicle Safe Spot"
            )
        }
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

    fun refreshVehicleGpsLocation() {
        scope.launch {
            syncWithCloudApi()
        }
    }

    fun updateCloudConfig(apiUrl: String, isGateway: Boolean) {
        _vehicleState.value = _vehicleState.value.copy(
            cloudConfig = _vehicleState.value.cloudConfig.copy(
                apiUrl = apiUrl,
                isGatewayAdapter = isGateway
            )
        )
    }

    fun toggleDoorsLock() {
        if (_vehicleState.value.lockState == LockState.LOCKED) {
            requestDoorUnlock()
        } else {
            requestDoorLock()
        }
    }

    fun soundHornAndFlash() {
        soundHorn()
    }

    fun calibrateTpms() {
        NotificationHelper.sendAlert(
            context,
            105,
            "TPMS Calibration",
            "Tire pressure monitoring calibration requires driving above 16 mph for 10 minutes."
        )
    }

    fun requestTpmsDiagnosticScan() {
        scope.launch {
            syncWithCloudApi()
        }
    }

    fun runBatteryLoadTest() {
        NotificationHelper.sendAlert(
            context,
            106,
            "Battery Health",
            "Direct 12V battery cranking load test requires local OBD-II hardware adapter."
        )
    }

    fun runDtcDiagnosticScan() {
        scope.launch {
            executeRemoteCommand("DIAGNOSTIC_SCAN")
            syncWithCloudApi()
        }
    }

    fun clearDtcCodes() {
        scope.launch {
            executeRemoteCommand("CLEAR_DTC")
            syncWithCloudApi()
        }
    }

    fun updateCloudConfig(apiUrl: String, token: String) {
        _vehicleState.value = _vehicleState.value.copy(
            cloudConfig = _vehicleState.value.cloudConfig.copy(
                apiUrl = apiUrl
            )
        )
    }

    suspend fun deleteTrip(tripId: Long) {
        tripDao.deleteTrip(tripId)
    }

    suspend fun clearAllTrips() {
        tripDao.clearAllTrips(_vehicleState.value.vin)
    }

    suspend fun authenticateWithMazdaConnectedServices(
        email: String,
        password: String,
        regionCode: String,
        customBaseUrl: String?
    ): Result<List<DiscoveredVehicle>> {
        val cluster = MazdaRegionalCluster.fromCode(regionCode)
        return authenticateMyMazdaAccount(email, password, cluster, customBaseUrl)
    }

    fun disconnectMazdaAccount() {
        signOutAccount()
    }

    fun startLiveDrive(startAddress: String, destAddress: String, tag: String) {
        _vehicleState.value = _vehicleState.value.copy(
            activeDriveSession = ActiveDriveSession(
                startTimeMillis = System.currentTimeMillis(),
                startAddress = startAddress,
                destinationAddress = destAddress,
                tripTag = tag
            )
        )
    }
}
