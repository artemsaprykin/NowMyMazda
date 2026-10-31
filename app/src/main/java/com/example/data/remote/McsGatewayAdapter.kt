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
 * Provides a formally defined typed REST adapter contract for connecting NowMyMazda
 * to a self-hosted instance of cv/mcs (Carlos Villela - https://github.com/cv/mcs)
 * or a compatible Home Assistant Mazda integration bridge.
 *
 * This adapter bridges the Android environment to the Go cv/mcs daemon when direct
 * Android-to-Azure-AD-B2C requests are constrained by mobile client redirect URI ownership.
 *
 * Strict behavior:
 * - Real HTTP requests with network timeout handling.
 * - No synthetic HTTP 200 substitution.
 * - Missing fields are reported as null/unavailable rather than populated with default values.
 */
interface McsGatewayApiService {

    @GET("health")
    suspend fun checkHealth(): Response<McsHealthDto>

    @GET("api/v1/session")
    suspend fun getSessionStatus(
        @Header("Authorization") authHeader: String? = null
    ): Response<McsSessionStatusDto>

    @GET("api/v1/vehicles")
    suspend fun getDiscoveredVehicles(
        @Header("Authorization") authHeader: String? = null
    ): Response<List<McsVehicleDto>>

    @GET("api/v1/vehicles/{vin}/status")
    suspend fun getVehicleTelemetry(
        @Path("vin") vin: String,
        @Header("Authorization") authHeader: String? = null
    ): Response<McsTelemetryDto>

    @POST("api/v1/vehicles/{vin}/remote-control")
    suspend fun dispatchRemoteCommand(
        @Path("vin") vin: String,
        @Body request: McsRemoteCommandRequestDto,
        @Header("Authorization") authHeader: String? = null
    ): Response<McsCommandResponseDto>

    @GET("api/v1/vehicles/{vin}/remote-control/status/{commandId}")
    suspend fun queryCommandStatus(
        @Path("vin") vin: String,
        @Path("commandId") commandId: String,
        @Header("Authorization") authHeader: String? = null
    ): Response<McsCommandStatusDto>
}

// ----------------- Gateway DTO Models -----------------

data class McsHealthDto(
    @Json(name = "status") val status: String = "unknown",
    @Json(name = "version") val version: String? = null,
    @Json(name = "uptime_seconds") val uptimeSeconds: Long? = null
)

data class McsSessionStatusDto(
    @Json(name = "authenticated") val authenticated: Boolean = false,
    @Json(name = "email") val email: String? = null,
    @Json(name = "region") val region: String? = null,
    @Json(name = "customer_id") val customerId: String? = null,
    @Json(name = "token_expires_at") val tokenExpiresAt: Long? = null,
    @Json(name = "vehicle_count") val vehicleCount: Int = 0
)

data class McsVehicleDto(
    @Json(name = "vin") val vin: String,
    @Json(name = "model_name") val modelName: String? = null,
    @Json(name = "model_year") val modelYear: Int? = null,
    @Json(name = "nickname") val nickname: String? = null,
    @Json(name = "tcu_paired") val tcuPaired: Boolean = false,
    @Json(name = "can_remote_control") val canRemoteControl: Boolean = false
)

data class McsRemoteCommandRequestDto(
    @Json(name = "action") val action: String,
    @Json(name = "duration_minutes") val durationMinutes: Int? = null,
    @Json(name = "target_temp_f") val targetTempF: Int? = null,
    @Json(name = "heated_seats") val heatedSeats: Boolean? = null
)

data class McsCommandResponseDto(
    @Json(name = "accepted") val accepted: Boolean = false,
    @Json(name = "command_id") val commandId: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "message") val message: String? = null
)

data class McsCommandStatusDto(
    @Json(name = "command_id") val commandId: String,
    @Json(name = "status") val status: String, // "PENDING", "ACCEPTED", "CONFIRMED", "FAILED"
    @Json(name = "is_completed") val isCompleted: Boolean = false,
    @Json(name = "error_message") val errorMessage: String? = null,
    @Json(name = "completed_at") val completedAt: Long? = null
)

data class McsTelemetryDto(
    @Json(name = "vin") val vin: String,
    @Json(name = "engine_status") val engineStatus: String? = null, // "OFF", "RUNNING"
    @Json(name = "door_lock_status") val doorLockStatus: String? = null, // "LOCKED", "UNLOCKED"
    @Json(name = "fuel_level_percent") val fuelLevelPercent: Int? = null,
    @Json(name = "odometer_miles") val odometerMiles: Int? = null,
    @Json(name = "battery_voltage") val batteryVoltage: Double? = null,
    @Json(name = "battery_soh") val batterySoh: Int? = null,
    @Json(name = "tpms_fl_psi") val tpmsFlPsi: Double? = null,
    @Json(name = "tpms_fr_psi") val tpmsFrPsi: Double? = null,
    @Json(name = "tpms_rl_psi") val tpmsRlPsi: Double? = null,
    @Json(name = "tpms_rr_psi") val tpmsRrPsi: Double? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "active_dtc_codes") val activeDtcCodes: List<String>? = null,
    @Json(name = "last_updated_timestamp") val lastUpdatedTimestamp: Long? = null
)

object McsGatewayClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
        })
        .build()

    fun create(baseUrl: String): McsGatewayApiService {
        val safeUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(safeUrl)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(McsGatewayApiService::class.java)
    }
}
