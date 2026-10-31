package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/**
 * MCS Gateway Adapter Interface.
 *
 * Defines the contract for an independently deployed cv/mcs (https://github.com/cv/mcs)
 * or Home Assistant Mazda REST daemon.
 *
 * Requirements from Phase 5:
 * - Base URL is normalized via normalizeGatewayUrl() so '/api/' is never duplicated.
 * - Does not claim the gateway is functional unless an actual authenticated daemon responds.
 * - Does not treat a simple HTTP health check as Mazda authentication.
 * - Never uses hardcoded placeholder tokens (such as "mcs_gateway_token").
 * - Requires explicit user authentication if configured on the gateway.
 * - Missing telemetry fields return null and are mapped to Unavailable/Not Supported in UI.
 */
interface McsGatewayApiService {

    @GET("health")
    suspend fun checkHealth(): Response<McsHealthDto>

    @GET("v1/session")
    suspend fun getSessionStatus(
        @Header("Authorization") authHeader: String? = null
    ): Response<McsSessionStatusDto>

    @GET("v1/vehicles")
    suspend fun getDiscoveredVehicles(
        @Header("Authorization") authHeader: String? = null
    ): Response<List<McsVehicleDto>>

    @GET("v1/vehicles/{vin}/status")
    suspend fun getVehicleTelemetry(
        @Path("vin") vin: String,
        @Header("Authorization") authHeader: String? = null
    ): Response<McsTelemetryDto>

    @POST("v1/vehicles/{vin}/remote-control")
    suspend fun dispatchRemoteCommand(
        @Path("vin") vin: String,
        @Body request: McsRemoteCommandRequestDto,
        @Header("Authorization") authHeader: String? = null
    ): Response<McsCommandResponseDto>

    @GET("v1/vehicles/{vin}/remote-control/status/{commandId}")
    suspend fun queryCommandStatus(
        @Path("vin") vin: String,
        @Path("commandId") commandId: String,
        @Header("Authorization") authHeader: String? = null
    ): Response<McsCommandStatusDto>
}

// ----------------- Gateway DTO Models -----------------

data class McsHealthDto(
    @param:Json(name = "status") val status: String = "unknown",
    @param:Json(name = "version") val version: String? = null,
    @param:Json(name = "uptime_seconds") val uptimeSeconds: Long? = null
)

data class McsSessionStatusDto(
    @param:Json(name = "authenticated") val authenticated: Boolean = false,
    @param:Json(name = "email") val email: String? = null,
    @param:Json(name = "region") val region: String? = null,
    @param:Json(name = "customer_id") val customerId: String? = null,
    @param:Json(name = "token_expires_at") val tokenExpiresAt: Long? = null,
    @param:Json(name = "vehicle_count") val vehicleCount: Int = 0
)

data class McsVehicleDto(
    @param:Json(name = "vin") val vin: String,
    @param:Json(name = "model_name") val modelName: String? = null,
    @param:Json(name = "model_year") val modelYear: Int? = null,
    @param:Json(name = "nickname") val nickname: String? = null,
    @param:Json(name = "tcu_paired") val tcuPaired: Boolean = false,
    @param:Json(name = "can_remote_control") val canRemoteControl: Boolean = false
)

data class McsRemoteCommandRequestDto(
    @param:Json(name = "action") val action: String,
    @param:Json(name = "duration_minutes") val durationMinutes: Int? = null,
    @param:Json(name = "target_temp_f") val targetTempF: Int? = null,
    @param:Json(name = "heated_seats") val heatedSeats: Boolean? = null
)

data class McsCommandResponseDto(
    @param:Json(name = "accepted") val accepted: Boolean = false,
    @param:Json(name = "command_id") val commandId: String? = null,
    @param:Json(name = "status") val status: String? = null,
    @param:Json(name = "message") val message: String? = null
)

data class McsCommandStatusDto(
    @param:Json(name = "command_id") val commandId: String,
    @param:Json(name = "status") val status: String, // "PENDING", "ACCEPTED", "CONFIRMED", "FAILED"
    @param:Json(name = "is_completed") val isCompleted: Boolean = false,
    @param:Json(name = "error_message") val errorMessage: String? = null,
    @param:Json(name = "completed_at") val completedAt: Long? = null
)

data class McsTelemetryDto(
    @param:Json(name = "vin") val vin: String,
    @param:Json(name = "engine_status") val engineStatus: String? = null, // "OFF", "RUNNING"
    @param:Json(name = "door_lock_status") val doorLockStatus: String? = null, // "LOCKED", "UNLOCKED"
    @param:Json(name = "fuel_level_percent") val fuelLevelPercent: Int? = null,
    @param:Json(name = "odometer_miles") val odometerMiles: Int? = null,
    @param:Json(name = "battery_voltage") val batteryVoltage: Double? = null,
    @param:Json(name = "battery_soh") val batterySoh: Int? = null,
    @param:Json(name = "tpms_fl_psi") val tpmsFlPsi: Double? = null,
    @param:Json(name = "tpms_fr_psi") val tpmsFrPsi: Double? = null,
    @param:Json(name = "tpms_rl_psi") val tpmsRlPsi: Double? = null,
    @param:Json(name = "tpms_rr_psi") val tpmsRrPsi: Double? = null,
    @param:Json(name = "latitude") val latitude: Double? = null,
    @param:Json(name = "longitude") val longitude: Double? = null,
    @param:Json(name = "active_dtc_codes") val activeDtcCodes: List<String>? = null,
    @param:Json(name = "last_updated_timestamp") val lastUpdatedTimestamp: Long? = null
)

object McsGatewayClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
        })
        .build()

    fun create(rawUrl: String): McsGatewayApiService {
        val safeUrl = MazdaRegionalCluster.normalizeGatewayUrl(rawUrl)
        return Retrofit.Builder()
            .baseUrl(safeUrl)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(McsGatewayApiService::class.java)
    }
}
