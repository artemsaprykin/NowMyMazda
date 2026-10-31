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
import com.example.data.model.LockState
import com.example.data.model.MyMazdaAccount
import com.example.data.model.PressureStatus
import com.example.data.model.RemoteStartSettings
import com.example.data.model.TPMSData
import com.example.data.model.TireInfo
import com.example.data.model.VehicleGpsLocation
import com.example.data.model.VehicleState
import com.example.data.remote.ApiClient
import com.example.data.remote.MazdaLoginRequest
import com.example.data.remote.MazdaRegionalCluster
import com.example.data.remote.MazdaRemoteCommandRequest
import com.example.data.remote.MazdaSecurityCrypto
import com.example.data.remote.RemoteControlRequest
import com.example.data.storage.AppPreferencesManager
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

class VehicleRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val database = AppDatabase.getInstance(context)
    private val tripDao = database.tripLogDao()
    private val maintenanceDao = database.maintenanceDao()

    val preferencesManager = AppPreferencesManager(context)
    val tokenStorage = SecureTokenStorage(context)

    // Starts in clean disconnected state with zero mock values
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
        val session = tokenStorage.loadSession()
        val isSessionValid = tokenStorage.hasValidSession()

        val regionCluster = MazdaRegionalCluster.fromCode(settings.accountRegionCode)
        val targetUrl = if (settings.customBridgeUrl.isNotBlank()) settings.customBridgeUrl else regionCluster.primaryBaseUrl

        if (isSessionValid && session != null && settings.accountEmail.isNotBlank()) {
            val isVinSelected = settings.selectedVin.isNotBlank()
            _vehicleState.value = _vehicleState.value.copy(
                vin = settings.selectedVin,
                nickname = if (isVinSelected) settings.selectedNickname.ifBlank { "Connected Mazda" } else "Select Vehicle",
                modelName = if (isVinSelected) settings.selectedNickname.ifBlank { "Mazda Connected Vehicle" } else "No Vehicle Selected",
                isVehicleSelected = isVinSelected,
                isConnected = true,
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
                    isConnected = true,
                    apiUrl = targetUrl,
                    vin = settings.selectedVin,
                    apiToken = session.accessToken,
                    regionEndpoint = regionCluster.regionTitle
                ),
                myMazdaAccount = MyMazdaAccount(
                    email = settings.accountEmail,
                    isLinked = true,
                    region = regionCluster.regionTitle,
                    regionCode = regionCluster.code,
                    customerId = session.customerId,
                    vehicleVin = settings.selectedVin,
                    dataSharingAgreementGranted = true,
                    remoteControlEntitlementActive = true,
                    lastHandshakeStatus = "Session restored from secure local Keystore"
                )
            )

            // Attempt telemetry sync for the active VIN
            if (isVinSelected) {
                syncWithCloudApi()
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
        val fuel = if (mpg > 0) Math.round((distance / mpg) * 100.0) / 100.0 else 0.0

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
                routeSummary = "$startAddress → $endAddress",
                fuelConsumedGal = fuel,
                tripTag = tag
            )
        )
    }

    suspend fun deleteTrip(tripId: Long) {
        tripDao.deleteTrip(tripId)
    }

    suspend fun clearAllTrips() {
        val vin = _vehicleState.value.vin
        if (vin.isNotBlank()) {
            tripDao.clearAllTrips(vin)
        }
    }

    // ----------------- Real Mazda Connected Services Authentication -----------------

    /**
     * Authenticates against the real Mazda Connected Services API (or cv/mcs bridge).
     * Never returns simulated success, placeholder tokens, or fake vehicles on failure.
     */
    suspend fun authenticateWithMazdaConnectedServices(
        email: String,
        password: String,
        regionCode: String,
        customBaseUrl: String? = null
    ): Result<List<DiscoveredVehicle>> = withContext(Dispatchers.IO) {
        val region = MazdaRegionalCluster.fromCode(regionCode)
        val targetBaseUrl = if (!customBaseUrl.isNullOrBlank()) {
            if (customBaseUrl.endsWith("/")) customBaseUrl else "$customBaseUrl/"
        } else {
            region.primaryBaseUrl
        }

        val startTime = System.currentTimeMillis()
        try {
            val apiService = ApiClient.createService(targetBaseUrl)

            // Step 1: Query dynamic public key for client-side RSA encryption (as in pymazda / mcs)
            val pkResponse = try {
                apiService.getPublicKey()
            } catch (e: Exception) {
                return@withContext Result.failure(
                    Exception("Failed to reach Mazda gateway at $targetBaseUrl: ${e.localizedMessage}")
                )
            }

            if (!pkResponse.isSuccessful || pkResponse.body() == null || pkResponse.body()!!.publicKey.isBlank()) {
                val code = pkResponse.code()
                return@withContext Result.failure(
                    Exception("Mazda public key retrieval failed (HTTP $code). Gateway returned invalid auth key.")
                )
            }

            val rawKey = pkResponse.body()!!.publicKey
            val keyTimestamp = pkResponse.body()!!.keyTimestamp

            // Step 2: Encrypt password client-side using RSA public key
            val encryptedPassword = try {
                MazdaSecurityCrypto.encryptPasswordWithPublicKey(password, rawKey)
            } catch (e: Exception) {
                return@withContext Result.failure(
                    Exception("Client-side RSA encryption failed: ${e.localizedMessage}")
                )
            }

            // Step 3: Send authenticated login request
            val loginReq = MazdaLoginRequest(
                email = email,
                encryptedPassword = encryptedPassword,
                keyTimestamp = keyTimestamp,
                region = region.code
            )

            val loginRes = try {
                apiService.login(loginReq)
            } catch (e: Exception) {
                return@withContext Result.failure(
                    Exception("Mazda login network request failed: ${e.localizedMessage}")
                )
            }

            val latency = System.currentTimeMillis() - startTime

            if (!loginRes.isSuccessful || loginRes.body() == null) {
                val code = loginRes.code()
                val errBody = loginRes.errorBody()?.string()
                val errorMsg = when (code) {
                    401 -> "Invalid Mazda account credentials. Please verify your email and password."
                    403 -> "Mazda authentication rejected (HTTP 403 Forbidden). Cloud security or regional block active."
                    429 -> "Too many login attempts. Please wait a few minutes before trying again."
                    else -> "Mazda authentication failed (HTTP $code): ${errBody ?: loginRes.message()}"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val loginData = loginRes.body()!!
            val token = loginData.accessToken
            if (token.isNullOrBlank()) {
                val errMsg = loginData.errorDescription ?: loginData.error ?: "Mazda auth server returned empty session token."
                return@withContext Result.failure(Exception(errMsg))
            }

            val customerId = loginData.userId ?: "CUST_${System.currentTimeMillis()}"
            val expiresInSeconds = loginData.expiresInSeconds ?: 86400L
            val expiryTime = System.currentTimeMillis() + (expiresInSeconds * 1000L)

            // Step 4: Persist tokens securely in Android KeyStore
            tokenStorage.saveSession(
                accessToken = token,
                refreshToken = loginData.refreshToken,
                tokenExpiry = expiryTime,
                customerId = customerId,
                region = region.code
            )

            // Step 5: Discover genuine vehicles linked to this Mazda account
            val vehiclesRes = try {
                apiService.getVehicles("Bearer $token")
            } catch (e: Exception) {
                return@withContext Result.failure(
                    Exception("Authentication succeeded, but vehicle discovery request failed: ${e.localizedMessage}")
                )
            }

            if (!vehiclesRes.isSuccessful || vehiclesRes.body() == null) {
                val code = vehiclesRes.code()
                return@withContext Result.failure(
                    Exception("Failed to retrieve vehicles for account (HTTP $code).")
                )
            }

            val vehicleItems = vehiclesRes.body()!!
            val discoveredList = vehicleItems.mapIndexed { index, item ->
                DiscoveredVehicle(
                    vin = item.vin,
                    modelName = if (!item.carlineName.isNullOrBlank()) "Mazda ${item.carlineName}" else "Mazda Vehicle",
                    modelYear = item.modelYear?.toIntOrNull() ?: 0,
                    nickname = item.nickname ?: if (!item.carlineName.isNullOrBlank()) "Mazda ${item.carlineName}" else item.vin,
                    isTcuPaired = item.tcuPaired,
                    canRemoteControl = item.canRemoteControl,
                    isSelected = index == 0
                )
            }

            val activeVehicle = discoveredList.firstOrNull()

            // Save settings to DataStore
            preferencesManager.saveAccountSettings(email, region.code, customBaseUrl)
            if (activeVehicle != null) {
                preferencesManager.saveSelectedVehicle(activeVehicle.vin, activeVehicle.nickname)
            }

            val updatedAccount = MyMazdaAccount(
                email = email,
                region = region.regionTitle,
                regionCode = region.code,
                customerId = customerId,
                vehicleCount = discoveredList.size,
                vehicleVin = activeVehicle?.vin ?: "",
                isLinked = true,
                isTcuPaired = activeVehicle?.isTcuPaired ?: false,
                lastLoginTimestamp = System.currentTimeMillis(),
                discoveredVehicles = discoveredList,
                dataSharingAgreementGranted = true,
                remoteControlEntitlementActive = activeVehicle?.canRemoteControl ?: false,
                inCarVerificationCompleted = activeVehicle?.isTcuPaired ?: false,
                safetyInterlocksClear = true,
                lastHandshakeStatus = "Authenticated via ${region.regionTitle} (${latency}ms, HTTP 200 OK)"
            )

            _vehicleState.value = _vehicleState.value.copy(
                vin = activeVehicle?.vin ?: "",
                modelName = activeVehicle?.modelName ?: "No Vehicle Selected",
                year = activeVehicle?.modelYear ?: 0,
                nickname = activeVehicle?.nickname ?: "No Vehicle",
                isVehicleSelected = activeVehicle != null,
                isConnected = true,
                myMazdaAccount = updatedAccount,
                cloudConfig = CloudApiConfig(
                    isConnected = true,
                    apiUrl = targetBaseUrl,
                    apiToken = token,
                    vin = activeVehicle?.vin ?: "",
                    lastHttpCode = 200,
                    lastLatencyMs = latency,
                    lastErrorMessage = null,
                    lastExecutedCommand = "Authenticated & Discovered (${discoveredList.size} vehicles)",
                    lastSyncTimestamp = System.currentTimeMillis(),
                    regionEndpoint = region.regionTitle
                )
            )

            NotificationHelper.sendAlert(
                context,
                301,
                "Mazda Account Connected",
                "Connected as $email. Retrieved ${discoveredList.size} genuine vehicle(s) from ${region.code}."
            )

            // Trigger telemetry sync for active vehicle if available
            if (activeVehicle != null) {
                syncWithCloudApi()
            }

            Result.success(discoveredList)
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            _vehicleState.value = _vehicleState.value.copy(
                cloudConfig = _vehicleState.value.cloudConfig.copy(
                    lastErrorMessage = "${e.javaClass.simpleName}: ${e.localizedMessage}",
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

        NotificationHelper.sendAlert(
            context,
            303,
            "Active Vehicle Switched",
            "Active vehicle set to ${selected.nickname} (${selected.vin})."
        )
    }

    fun disconnectMazdaAccount() {
        tokenStorage.clearSession()
        scope.launch {
            preferencesManager.clearAccountAndVehicle()
        }

        _vehicleState.value = VehicleState(
            themeMode = _vehicleState.value.themeMode,
            cloudConfig = CloudApiConfig(isConnected = false, lastExecutedCommand = "Account Signed Out"),
            myMazdaAccount = MyMazdaAccount(lastHandshakeStatus = "Account signed out")
        )

        NotificationHelper.sendAlert(
            context,
            304,
            "Signed Out",
            "Mazda account session and tokens cleared securely from device."
        )
    }

    suspend fun testConnectionHandshake(): String = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val service = ApiClient.createService(_vehicleState.value.cloudConfig.apiUrl)
            val session = tokenStorage.loadSession()
            val token = session?.accessToken ?: _vehicleState.value.cloudConfig.apiToken

            val res = if (token.isNotBlank()) {
                service.pingGateway("Bearer $token")
            } else {
                service.getPublicKey()
            }

            val latency = System.currentTimeMillis() - start
            val code = res.code()
            val isSuccess = res.isSuccessful

            _vehicleState.value = _vehicleState.value.copy(
                cloudConfig = _vehicleState.value.cloudConfig.copy(
                    lastHttpCode = code,
                    lastLatencyMs = latency,
                    lastSyncTimestamp = System.currentTimeMillis(),
                    lastExecutedCommand = "Handshake Ping (HTTP $code)",
                    lastErrorMessage = if (isSuccess) null else "HTTP $code: ${res.message()}"
                )
            )

            if (isSuccess) {
                "Handshake OK: Gateway responded in ${latency}ms (HTTP $code). Gateway link active."
            } else {
                "Handshake Warning: Gateway returned HTTP $code (${res.message()}) in ${latency}ms."
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            _vehicleState.value = _vehicleState.value.copy(
                cloudConfig = _vehicleState.value.cloudConfig.copy(
                    lastHttpCode = null,
                    lastLatencyMs = latency,
                    lastErrorMessage = "${e.javaClass.simpleName}: ${e.localizedMessage ?: "Connection error"}"
                )
            )
            "Handshake Error (${latency}ms): ${e.localizedMessage ?: "Unable to connect to gateway"}"
        }
    }

    // ----------------- Real Remote Commands -----------------

    fun remoteStartEngine(durationMinutes: Int, climateTempF: Int) {
        if (!_vehicleState.value.isVehicleSelected || _vehicleState.value.vin.isBlank()) {
            NotificationHelper.sendAlert(context, 101, "Command Rejected", "No Mazda vehicle is currently selected.")
            return
        }

        _vehicleState.value = _vehicleState.value.copy(
            commandExecutionState = CommandExecutionState.Pending("Remote Engine Start", "Sending ignition request to Mazda Connected Services...")
        )

        scope.launch {
            val result = executeRemoteCommand(
                action = "START_ENGINE",
                durationMinutes = durationMinutes,
                tempF = climateTempF
            )

            result.fold(
                onSuccess = { responseMsg ->
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Accepted(
                            "Remote Engine Start",
                            responseMsg.ifBlank { "Ignition request accepted by cloud. Awaiting TCU verification..." }
                        )
                    )

                    NotificationHelper.sendAlert(
                        context,
                        101,
                        "Remote Start Dispatched",
                        "Command accepted by Mazda cloud. Polling vehicle TCU status...",
                        isUrgent = false
                    )

                    // Poll vehicle status to confirm physical ignition result
                    pollForCommandConfirmation(
                        expectedCondition = { state -> state.engineState == EngineState.RUNNING },
                        actionName = "Remote Engine Start",
                        successMsg = "Engine ignition confirmed active by TCU."
                    )
                },
                onFailure = { error ->
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Failed(
                            "Remote Engine Start",
                            error.localizedMessage ?: "Remote start failed"
                        )
                    )
                    NotificationHelper.sendAlert(
                        context,
                        101,
                        "Remote Start Failed",
                        error.localizedMessage ?: "Request failed. Vehicle status unchanged.",
                        isUrgent = true
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
                onSuccess = { msg ->
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Accepted("Engine Stop", msg)
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
            commandExecutionState = CommandExecutionState.Pending("Door Lock", "Sending lock command to vehicle TCU...")
        )

        scope.launch {
            val result = executeRemoteCommand(action = "LOCK")
            result.fold(
                onSuccess = { msg ->
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Accepted("Door Lock", msg)
                    )
                    pollForCommandConfirmation(
                        expectedCondition = { state -> state.lockState == LockState.LOCKED },
                        actionName = "Door Lock",
                        successMsg = "Vehicle doors confirmed LOCKED."
                    )
                },
                onFailure = { error ->
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Failed("Door Lock", error.localizedMessage ?: "Lock request failed")
                    )
                }
            )
        }
    }

    fun requestDoorUnlock() {
        if (!_vehicleState.value.isVehicleSelected || _vehicleState.value.vin.isBlank()) return

        _vehicleState.value = _vehicleState.value.copy(
            commandExecutionState = CommandExecutionState.Pending("Door Unlock", "Sending unlock command to vehicle TCU...")
        )

        scope.launch {
            val result = executeRemoteCommand(action = "UNLOCK")
            result.fold(
                onSuccess = { msg ->
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Accepted("Door Unlock", msg)
                    )
                    pollForCommandConfirmation(
                        expectedCondition = { state -> state.lockState == LockState.UNLOCKED },
                        actionName = "Door Unlock",
                        successMsg = "Vehicle doors confirmed UNLOCKED."
                    )
                },
                onFailure = { error ->
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Failed("Door Unlock", error.localizedMessage ?: "Unlock request failed")
                    )
                }
            )
        }
    }

    fun toggleDoorsLock() {
        if (_vehicleState.value.lockState == LockState.LOCKED) {
            requestDoorUnlock()
        } else {
            requestDoorLock()
        }
    }

    fun toggleHazardLights() {
        val targetAction = if (!_vehicleState.value.hazardLightsActive) "FLASH_HAZARDS_ON" else "FLASH_HAZARDS_OFF"
        scope.launch {
            executeRemoteCommand(targetAction)
        }
    }

    fun soundHornAndFlash() {
        scope.launch {
            executeRemoteCommand("SOUND_HORN_AND_FLASH")
        }
    }

    fun toggleTrunk() {
        scope.launch {
            executeRemoteCommand("TOGGLE_TRUNK")
        }
    }

    fun calibrateTpms() {
        scope.launch {
            executeRemoteCommand("RECALIBRATE_TPMS")
            NotificationHelper.sendAlert(
                context,
                104,
                "TPMS Calibration",
                "TPMS calibration command sent. Values will update on next telematics report."
            )
        }
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

    private suspend fun executeRemoteCommand(
        action: String,
        durationMinutes: Int? = null,
        tempF: Int? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val vin = _vehicleState.value.vin
        if (vin.isBlank()) {
            return@withContext Result.failure(Exception("No active vehicle selected."))
        }

        val session = tokenStorage.loadSession()
        val token = session?.accessToken ?: _vehicleState.value.cloudConfig.apiToken
        if (token.isBlank()) {
            return@withContext Result.failure(Exception("Active Mazda Connected Services session required."))
        }

        val startTime = System.currentTimeMillis()
        try {
            val service = ApiClient.createService(_vehicleState.value.cloudConfig.apiUrl)
            val authHeader = "Bearer $token"

            val response = when (action) {
                "START_ENGINE" -> service.remoteStartEngine(
                    token = authHeader,
                    vin = vin,
                    request = MazdaRemoteCommandRequest(
                        vin = vin,
                        action = action,
                        durationMinutes = durationMinutes ?: 15,
                        targetTempF = tempF ?: 72
                    )
                )
                "STOP_ENGINE" -> service.remoteStopEngine(authHeader, vin)
                "LOCK" -> service.remoteLockDoors(authHeader, vin)
                "UNLOCK" -> service.remoteUnlockDoors(authHeader, vin)
                "FLASH_HAZARDS_ON", "FLASH_HAZARDS_OFF" -> service.remoteHazardLights(authHeader, vin)
                "SOUND_HORN_AND_FLASH" -> service.remoteHorn(authHeader, vin)
                "TOGGLE_TRUNK" -> service.remoteTrunk(authHeader, vin)
                "DIAGNOSTIC_SCAN" -> service.runDiagnosticScan(authHeader, vin)
                "CLEAR_DTC" -> service.clearDtcCodes(authHeader, vin)
                else -> {
                    // Fallback to legacy/bridge route
                    service.executeControlCommand(
                        token = authHeader,
                        request = RemoteControlRequest(vin = vin, action = action, durationMinutes = durationMinutes, climateTempF = tempF)
                    )
                }
            }

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
                val message = body?.message ?: "Command $action accepted by Mazda cloud gateway."
                Result.success(message)
            } else {
                val errorMsg = when (code) {
                    401 -> "Session expired. Please reconnect your Mazda account."
                    403 -> "Remote command disallowed by Mazda (vehicle interlocks engaged or ignition locked)."
                    404 -> "Vehicle VIN not found on this regional cluster."
                    else -> "Command rejected by server (HTTP $code: ${response.message()})"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            _vehicleState.value = _vehicleState.value.copy(
                cloudConfig = _vehicleState.value.cloudConfig.copy(
                    lastHttpCode = null,
                    lastLatencyMs = latency,
                    lastExecutedCommand = "$action",
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
            // Give TCU time to receive network command (Mazda TCU polling cycle takes 4-8s)
            delay(4000)
            var attempts = 0
            while (attempts < 3) {
                syncWithCloudApi()
                if (expectedCondition(_vehicleState.value)) {
                    _vehicleState.value = _vehicleState.value.copy(
                        commandExecutionState = CommandExecutionState.Confirmed(actionName, successMsg)
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
            // If condition was not yet verified, transition back to Idle
            _vehicleState.value = _vehicleState.value.copy(
                commandExecutionState = CommandExecutionState.Idle
            )
        }
    }

    // ----------------- Real Telematics Status Sync -----------------

    suspend fun syncWithCloudApi(): Boolean = withContext(Dispatchers.IO) {
        val vin = _vehicleState.value.vin
        if (vin.isBlank()) return@withContext false

        val session = tokenStorage.loadSession()
        val token = session?.accessToken ?: _vehicleState.value.cloudConfig.apiToken
        if (token.isBlank()) return@withContext false

        _vehicleState.value = _vehicleState.value.copy(
            cloudConfig = _vehicleState.value.cloudConfig.copy(isSyncing = true)
        )
        val startTime = System.currentTimeMillis()
        try {
            val service = ApiClient.createService(_vehicleState.value.cloudConfig.apiUrl)
            val authHeader = "Bearer $token"

            val response = try {
                service.getVehicleStatus(authHeader, vin)
            } catch (_: Exception) {
                // Fallback to bridge telematics endpoint if direct status 404s
                service.getVehicleTelematics(authHeader, vin)
            }

            val latency = System.currentTimeMillis() - startTime

            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!

                val mappedEngine = when (dto.engineStatus?.uppercase()) {
                    "RUNNING", "ON" -> EngineState.RUNNING
                    "STARTING" -> EngineState.STARTING
                    else -> EngineState.OFF
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
                        stateOfHealthPercent = dto.batterySoh ?: 90,
                        stateOfChargePercent = ((dto.batteryVoltage - 11.8) / (12.7 - 11.8) * 100.0).toInt().coerceIn(0, 100),
                        alternatorStatus = if (mappedEngine == EngineState.RUNNING) "${dto.batteryVoltage}V (Charging)" else "${dto.batteryVoltage}V (Resting)",
                        healthSummary = if (dto.batteryVoltage >= 12.2) "Good Condition" else "Low Voltage Alert"
                    )
                } else {
                    _vehicleState.value.batteryHealth
                }

                val hasTpms = dto.tpmsFlPsi != null && dto.tpmsFlPsi > 0.0
                val updatedTpms = if (hasTpms) {
                    TPMSData(
                        isAvailable = true,
                        frontLeft = TireInfo("FL", "Front Left", dto.tpmsFlPsi!!, status = if (dto.tpmsFlPsi < 30.0) PressureStatus.LOW else PressureStatus.NORMAL, isAvailable = true),
                        frontRight = TireInfo("FR", "Front Right", dto.tpmsFrPsi ?: dto.tpmsFlPsi, status = PressureStatus.NORMAL, isAvailable = true),
                        rearLeft = TireInfo("RL", "Rear Left", dto.tpmsRlPsi ?: dto.tpmsFlPsi, status = PressureStatus.NORMAL, isAvailable = true),
                        rearRight = TireInfo("RR", "Rear Right", dto.tpmsRrPsi ?: dto.tpmsFlPsi, status = PressureStatus.NORMAL, isAvailable = true)
                    )
                } else {
                    _vehicleState.value.tpms
                }

                val hasGps = dto.latitude != null && dto.longitude != null && dto.latitude != 0.0
                val updatedLocation = if (hasGps) {
                    VehicleGpsLocation(
                        isAvailable = true,
                        latitude = dto.latitude!!,
                        longitude = dto.longitude!!,
                        address = "GPS: ${String.format(java.util.Locale.US, "%.4f, %.4f", dto.latitude, dto.longitude)}",
                        lastUpdatedTimestamp = dto.timestamp ?: System.currentTimeMillis()
                    )
                } else {
                    _vehicleState.value.location
                }

                val updatedDiagnostics = DiagnosticStatus(
                    isAvailable = true,
                    checkEngineOn = dto.dtcCodes.isNotEmpty(),
                    oilLifePercent = dto.oilLifePercent ?: _vehicleState.value.diagnostics.oilLifePercent,
                    activeDtcCodes = dto.dtcCodes,
                    lastScanTimestamp = System.currentTimeMillis(),
                    scanStatusMessage = if (dto.dtcCodes.isEmpty()) "All ECU modules report clean status" else "Active DTC codes detected: ${dto.dtcCodes.joinToString()}"
                )

                _vehicleState.value = _vehicleState.value.copy(
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
        val addr = address ?: "Current GPS (${String.format(java.util.Locale.US, "%.4f, %.4f", lat, lng)})"
        _vehicleState.value = _vehicleState.value.copy(
            location = _vehicleState.value.location.copy(
                isAvailable = true,
                latitude = lat,
                longitude = lng,
                address = addr,
                lastUpdatedTimestamp = System.currentTimeMillis()
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

    fun updateCloudConfig(apiUrl: String, token: String) {
        _vehicleState.value = _vehicleState.value.copy(
            cloudConfig = _vehicleState.value.cloudConfig.copy(
                apiUrl = apiUrl,
                apiToken = token
            )
        )
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
