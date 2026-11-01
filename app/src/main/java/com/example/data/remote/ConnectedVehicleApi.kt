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
 * Explicit Connection Mode separation as mandated by Phase 4:
 * DIRECT_MAZDA: Communicates with genuine Mazda cloud infrastructure.
 * MCS_GATEWAY: Communicates with an independently hosted cv/mcs or Home Assistant daemon.
 */
enum class ConnectionMode {
    DIRECT_MAZDA,
    MCS_GATEWAY
}

/**
 * Mazda Connected Services Regional Endpoints.
 * Source of truth:
 * - Official MyMazda Android app (com.interrait.mymazda) network traffic capture (PCAPdroid/Wireshark)
 * - cv/mcs (Carlos Villela - https://github.com/cv/mcs) v0.7.12
 *
 * MNAO is verified with captured traffic and live DNS resolution.
 * MME and MJO are kept explicitly unverified until confirmed with active telemetry captures.
 * Obsolete hosts (app-api.mazdausa.com, eu.mzda.net, jp.mzda.net) have been completely removed.
 */
enum class MazdaRegionalCluster(
    val code: String,
    val regionTitle: String,
    val primaryBaseUrl: String,
    val authHost: String,
    val additionalBackendHost: String?,
    val isVerified: Boolean,
    val description: String
) {
    MNAO(
        code = "MNAO",
        regionTitle = "North America (MNAO)",
        primaryBaseUrl = "https://0cxo7m58.mazda.com/prod/",
        authHost = "na.id.mazda.com",
        additionalBackendHost = "hgs2ivna.mazda.com",
        isVerified = true,
        description = "United States & Canada Mazda Connected Services (Verified Production Host)"
    ),
    MME(
        code = "MME",
        regionTitle = "Europe (MME)",
        primaryBaseUrl = "https://e9stj7g7.mazda.com/prod/",
        authHost = "login.microsoftonline.com",
        additionalBackendHost = null,
        isVerified = false,
        description = "Mazda Motor Europe Regional Cloud (UNVERIFIED)"
    ),
    MJO(
        code = "MJO",
        regionTitle = "Japan (MJO)",
        primaryBaseUrl = "https://wcs9p6wj.mazda.com/prod/",
        authHost = "login.microsoftonline.com",
        additionalBackendHost = null,
        isVerified = false,
        description = "Mazda Motor Corporation Japan Cloud (UNVERIFIED)"
    ),
    CV_MCS_BRIDGE(
        code = "BRIDGE",
        regionTitle = "cv/mcs Gateway Adapter",
        primaryBaseUrl = "http://10.0.2.2:8080/",
        authHost = "10.0.2.2",
        additionalBackendHost = null,
        isVerified = false,
        description = "Self-hosted cv/mcs or Home Assistant daemon proxy"
    );

    companion object {
        fun fromCode(code: String): MazdaRegionalCluster {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: MNAO
        }

        /**
         * Cleanses user-provided gateway URLs to prevent duplicated '/api/' path segments.
         */
        fun normalizeGatewayUrl(rawUrl: String): String {
            var trimmed = rawUrl.trim()
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                trimmed = "http://$trimmed"
            }
            // Strip trailing slashes
            while (trimmed.endsWith("/")) {
                trimmed = trimmed.substring(0, trimmed.length - 1)
            }
            // If user supplied /api, strip it so Retrofit base URL doesn't double it
            if (trimmed.endsWith("/api")) {
                trimmed = trimmed.substring(0, trimmed.length - 4)
            }
            return "$trimmed/"
        }
    }
}

// ----------------- Real Mazda Connected Services Request & Response Envelopes -----------------

/**
 * Direct Mazda Connected Services API request envelope.
 * As documented in cv/mcs internal/api/client.go, all direct calls require device identification
 * and standard client headers.
 */
data class MazdaApiEnvelope<T>(
    @Json(name = "header") val header: MazdaApiHeaderDto,
    @Json(name = "body") val body: T
)

data class MazdaApiHeaderDto(
    @Json(name = "appVersion") val appVersion: String = "9.5.0",
    @Json(name = "appPackage") val appPackage: String = "com.interrait.mymazda",
    @Json(name = "platform") val platform: String = "Android",
    @Json(name = "deviceId") val deviceId: String,
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

data class MazdaVehicleListRequest(
    @Json(name = "customerId") val customerId: String? = null
)

data class MazdaVehicleListResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "errorCode") val errorCode: String? = null,
    @Json(name = "errorMessage") val errorMessage: String? = null,
    @Json(name = "vehicles") val vehicles: List<MazdaVehicleDtoItem>? = null
)

data class MazdaVehicleDtoItem(
    @Json(name = "vin") val vin: String,
    @Json(name = "carlineName") val carlineName: String? = null,
    @Json(name = "modelYear") val modelYear: String? = null,
    @Json(name = "nickname") val nickname: String? = null,
    @Json(name = "colorCode") val colorCode: String? = null,
    @Json(name = "tcuPaired") val tcuPaired: Boolean? = null,
    @Json(name = "canRemoteControl") val canRemoteControl: Boolean? = null
)

data class MazdaStatusRequest(
    @Json(name = "vin") val vin: String
)

data class MazdaTelematicsResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "errorCode") val errorCode: String? = null,
    @Json(name = "errorMessage") val errorMessage: String? = null,
    @Json(name = "reportTimestamp") val reportTimestamp: Long? = null,
    @Json(name = "vehicleStatus") val vehicleStatus: MazdaVehicleStatusPayload? = null
)

data class MazdaVehicleStatusPayload(
    @Json(name = "vin") val vin: String? = null,
    @Json(name = "engineRunning") val engineRunning: Boolean? = null,
    @Json(name = "doorLockStatus") val doorLockStatus: String? = null,
    @Json(name = "doors") val doors: MazdaDoorStatusPayload? = null,
    @Json(name = "tpms") val tpms: MazdaTpmsPayload? = null,
    @Json(name = "fuelRemainingPercent") val fuelRemainingPercent: Int? = null,
    @Json(name = "odometerMiles") val odometerMiles: Int? = null,
    @Json(name = "batteryVoltage") val batteryVoltage: Double? = null,
    @Json(name = "battery12vSoh") val battery12vSoh: Int? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "activeDtcCodes") val activeDtcCodes: List<String>? = null
)

data class MazdaDoorStatusPayload(
    @param:Json(name = "driverDoorOpen") val driverDoorOpen: Boolean? = null,
    @param:Json(name = "passengerDoorOpen") val passengerDoorOpen: Boolean? = null,
    @param:Json(name = "rearLeftDoorOpen") val rearLeftDoorOpen: Boolean? = null,
    @param:Json(name = "rearRightDoorOpen") val rearRightDoorOpen: Boolean? = null,
    @param:Json(name = "trunkOpen") val trunkOpen: Boolean? = null,
    @param:Json(name = "hoodOpen") val hoodOpen: Boolean? = null,
    @param:Json(name = "windowsOpen") val windowsOpen: Boolean? = null
)

data class MazdaTpmsPayload(
    @param:Json(name = "flPressurePsi") val flPressurePsi: Double? = null,
    @param:Json(name = "frPressurePsi") val frPressurePsi: Double? = null,
    @param:Json(name = "rlPressurePsi") val rlPressurePsi: Double? = null,
    @param:Json(name = "rrPressurePsi") val rrPressurePsi: Double? = null
)

data class MazdaRemoteControlRequestPayload(
    @param:Json(name = "vin") val vin: String,
    @param:Json(name = "command") val command: String,
    @param:Json(name = "durationMinutes") val durationMinutes: Int? = null,
    @param:Json(name = "targetTempF") val targetTempF: Int? = null
)

data class MazdaRemoteControlResponsePayload(
    @param:Json(name = "status") val status: String? = null,
    @param:Json(name = "commandId") val commandId: String? = null,
    @param:Json(name = "accepted") val accepted: Boolean? = null,
    @param:Json(name = "errorCode") val errorCode: String? = null,
    @param:Json(name = "errorMessage") val errorMessage: String? = null
)

data class MazdaCommandStatusPayload(
    @param:Json(name = "commandId") val commandId: String,
    @param:Json(name = "state") val state: String? = null, // "PENDING", "ACCEPTED", "SUCCESS", "FAILED"
    @param:Json(name = "completed") val completed: Boolean? = null,
    @param:Json(name = "errorCode") val errorCode: String? = null,
    @param:Json(name = "errorMessage") val errorMessage: String? = null
)

// ----------------- Retrofit Direct Mazda API Service -----------------

/**
 * Direct Mazda Connected Services API service interface for MNAO cluster.
 * Host: https://0cxo7m58.mazda.com/prod/
 */
interface MazdaDirectApiService {

    @POST("v1/vehicles/list")
    suspend fun getVehicleList(
        @Header("Authorization") authorization: String,
        @Header("X-Device-Id") deviceId: String,
        @Body request: MazdaApiEnvelope<MazdaVehicleListRequest>
    ): Response<MazdaVehicleListResponse>

    @POST("v1/vehicles/status")
    suspend fun getVehicleStatus(
        @Header("Authorization") authorization: String,
        @Header("X-Device-Id") deviceId: String,
        @Body request: MazdaApiEnvelope<MazdaStatusRequest>
    ): Response<MazdaTelematicsResponse>

    @POST("v1/vehicles/remote-control")
    suspend fun dispatchRemoteCommand(
        @Header("Authorization") authorization: String,
        @Header("X-Device-Id") deviceId: String,
        @Body request: MazdaApiEnvelope<MazdaRemoteControlRequestPayload>
    ): Response<MazdaRemoteControlResponsePayload>

    @POST("v1/vehicles/remote-control/status")
    suspend fun queryRemoteCommandStatus(
        @Header("Authorization") authorization: String,
        @Header("X-Device-Id") deviceId: String,
        @Body request: MazdaApiEnvelope<MazdaCommandStatusPayload>
    ): Response<MazdaCommandStatusPayload>
}

// ----------------- Client Factory -----------------

object MazdaDirectApiClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
            redactHeader("X-Device-Id")
        })
        .build()

    fun create(baseUrl: String): MazdaDirectApiService {
        val safeUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(safeUrl)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MazdaDirectApiService::class.java)
    }
}
