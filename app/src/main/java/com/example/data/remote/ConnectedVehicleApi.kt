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
 * Mazda Connected Services Regional Endpoints as referenced in:
 * - https://github.com/cv/mcs
 * - https://github.com/cv/mcs/blob/main/internal/api/auth.go
 * - https://github.com/cv/mcs/blob/main/internal/api/b2c.go
 */
enum class MazdaRegionalCluster(
    val code: String,
    val regionTitle: String,
    val primaryBaseUrl: String,
    val description: String
) {
    MNAO(
        code = "MNAO",
        regionTitle = "North America (MNAO)",
        primaryBaseUrl = "https://app-api.mazdausa.com/",
        description = "United States & Canada Mazda Connected Services"
    ),
    MME(
        code = "MME",
        regionTitle = "Europe (MME)",
        primaryBaseUrl = "https://eu.mzda.net/api/",
        description = "Mazda Motor Europe Regional Cloud"
    ),
    MJO(
        code = "MJO",
        regionTitle = "Japan (MJO)",
        primaryBaseUrl = "https://jp.mzda.net/api/",
        description = "Mazda Motor Corporation Japan Cloud"
    ),
    CV_MCS_BRIDGE(
        code = "BRIDGE",
        regionTitle = "cv/mcs Gateway Adapter",
        primaryBaseUrl = "http://10.0.2.2:8080/api/",
        description = "Local or networked cv/mcs Go daemon / proxy"
    );

    companion object {
        fun fromCode(code: String): MazdaRegionalCluster {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: MNAO
        }
    }
}

// ----------------- Data Transfer Objects (DTOs) -----------------

data class MazdaVehicleItem(
    @Json(name = "vin") val vin: String,
    @Json(name = "carlineName") val carlineName: String? = null,
    @Json(name = "modelYear") val modelYear: String? = null,
    @Json(name = "nickname") val nickname: String? = null,
    @Json(name = "colorCode") val colorCode: String? = null,
    @Json(name = "tcuPaired") val tcuPaired: Boolean = false,
    @Json(name = "canRemoteControl") val canRemoteControl: Boolean = false
)

data class MazdaRemoteCommandRequest(
    @Json(name = "vin") val vin: String,
    @Json(name = "action") val action: String,
    @Json(name = "durationMinutes") val durationMinutes: Int? = null,
    @Json(name = "targetTempF") val targetTempF: Int? = null,
    @Json(name = "heatedSeats") val heatedSeats: Boolean? = null
)

data class RemoteControlRequest(
    @Json(name = "vin") val vin: String,
    @Json(name = "action") val action: String,
    @Json(name = "duration_minutes") val durationMinutes: Int? = null,
    @Json(name = "climate_temp_f") val climateTempF: Int? = null,
    @Json(name = "heated_seats") val heatedSeats: Boolean? = null
)

data class RemoteControlResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "command_id") val commandId: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "message") val message: String? = null
)

data class CommandStatusResponse(
    @Json(name = "command_id") val commandId: String,
    @Json(name = "status") val status: String,
    @Json(name = "completed") val completed: Boolean = false,
    @Json(name = "error_message") val errorMessage: String? = null
)

data class CloudTelematicsDto(
    @Json(name = "vin") val vin: String,
    @Json(name = "engine_status") val engineStatus: String? = null,
    @Json(name = "door_lock_status") val doorLockStatus: String? = null,
    @Json(name = "fuel_level_percent") val fuelLevelPercent: Int? = null,
    @Json(name = "battery_voltage") val batteryVoltage: Double? = null,
    @Json(name = "battery_soh") val batterySoh: Int? = null,
    @Json(name = "tpms_fl_psi") val tpmsFlPsi: Double? = null,
    @Json(name = "tpms_fr_psi") val tpmsFrPsi: Double? = null,
    @Json(name = "tpms_rl_psi") val tpmsRlPsi: Double? = null,
    @Json(name = "tpms_rr_psi") val tpmsRrPsi: Double? = null,
    @Json(name = "oil_life_percent") val oilLifePercent: Int? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "odometer_miles") val odometerMiles: Int? = null,
    @Json(name = "active_dtc_codes") val dtcCodes: List<String>? = null,
    @Json(name = "timestamp") val timestamp: Long? = null
)

// ----------------- Retrofit API Interface -----------------

interface ConnectedVehicleApiService {

    // --- Vehicle Discovery & Status ---

    @GET("api/v1/vehicles")
    suspend fun getVehicles(
        @Header("Authorization") token: String
    ): Response<List<MazdaVehicleItem>>

    @GET("api/v1/vehicles/{vin}/status")
    suspend fun getVehicleStatus(
        @Header("Authorization") token: String,
        @Path("vin") vin: String
    ): Response<CloudTelematicsDto>

    // --- Remote Control Commands ---

    @POST("api/v1/vehicles/{vin}/remote-control")
    suspend fun dispatchRemoteCommand(
        @Header("Authorization") token: String,
        @Path("vin") vin: String,
        @Body request: RemoteControlRequest
    ): Response<RemoteControlResponse>

    @GET("api/v1/vehicles/{vin}/remote-control/status/{commandId}")
    suspend fun getRemoteCommandStatus(
        @Header("Authorization") token: String,
        @Path("vin") vin: String,
        @Path("commandId") commandId: String
    ): Response<CommandStatusResponse>

    @GET("api/v1/health")
    suspend fun pingGateway(
        @Header("Authorization") token: String
    ): Response<RemoteControlResponse>
}

// ----------------- Retrofit Client Builder -----------------

object ApiClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
        })
        .build()

    fun createService(baseUrl: String): ConnectedVehicleApiService {
        val safeUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(safeUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ConnectedVehicleApiService::class.java)
    }
}
