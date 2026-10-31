package com.example.data.remote

import android.util.Base64
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
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher

/**
 * Mazda Connected Services Regional Endpoints as implemented by:
 * - cv/mcs (Carlos Villela - https://github.com/cv/mcs)
 * - fano0001/home-assistant-mazda (pymazda - https://github.com/fano0001/home-assistant-mazda)
 * - tkarle/home-assistant-mazda (https://github.com/tkarle/home-assistant-mazda)
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
        regionTitle = "cv/mcs Local Daemon / Proxy",
        primaryBaseUrl = "http://10.0.2.2:8080/api/",
        description = "Self-hosted Go cv/mcs or Home Assistant local proxy"
    );

    companion object {
        fun fromCode(code: String): MazdaRegionalCluster {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: MNAO
        }
    }
}

/**
 * Authentication and encryption helper matching the Mazda mobile app & pymazda protocol.
 * Passwords are encrypted client-side using an RSA Public Key retrieved dynamically from Mazda's auth server.
 */
object MazdaSecurityCrypto {
    fun encryptPasswordWithPublicKey(password: String, rawPublicKeyPem: String): String {
        val sanitized = rawPublicKeyPem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")
            .trim()

        val decodedKey = Base64.decode(sanitized, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(decodedKey)
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKey = keyFactory.generatePublic(keySpec)

        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encryptedBytes = cipher.doFinal(password.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
    }
}

// ----------------- Data Transfer Objects (DTOs) -----------------

data class PublicKeyResponse(
    @Json(name = "publicKey") val publicKey: String = "",
    @Json(name = "keyTimestamp") val keyTimestamp: Long = System.currentTimeMillis()
)

data class MazdaLoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "encryptedPassword") val encryptedPassword: String,
    @Json(name = "keyTimestamp") val keyTimestamp: Long = System.currentTimeMillis(),
    @Json(name = "region") val region: String = "MNAO",
    @Json(name = "deviceId") val deviceId: String = "android_nowmymazda_app"
)

data class MazdaLoginResponse(
    @Json(name = "accessToken") val accessToken: String? = null,
    @Json(name = "refreshToken") val refreshToken: String? = null,
    @Json(name = "userId") val userId: String? = null,
    @Json(name = "expiresIn") val expiresInSeconds: Long? = null,
    @Json(name = "customerName") val customerName: String? = null,
    @Json(name = "region") val region: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "error_description") val errorDescription: String? = null
)

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
    @Json(name = "active_dtc_codes") val dtcCodes: List<String> = emptyList(),
    @Json(name = "timestamp") val timestamp: Long? = null
)

// ----------------- Retrofit API Interface -----------------

interface ConnectedVehicleApiService {

    // --- Authentication Endpoints (cv/mcs & pymazda protocol) ---

    @GET("api/v1/auth/publicKey")
    suspend fun getPublicKey(): Response<PublicKeyResponse>

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: MazdaLoginRequest
    ): Response<MazdaLoginResponse>

    // --- Vehicle Discovery & Status Endpoints ---

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

    @POST("api/v1/vehicles/{vin}/remote/engine/start")
    suspend fun remoteStartEngine(
        @Header("Authorization") token: String,
        @Path("vin") vin: String,
        @Body request: MazdaRemoteCommandRequest
    ): Response<RemoteControlResponse>

    @POST("api/v1/vehicles/{vin}/remote/engine/stop")
    suspend fun remoteStopEngine(
        @Header("Authorization") token: String,
        @Path("vin") vin: String
    ): Response<RemoteControlResponse>

    @POST("api/v1/vehicles/{vin}/remote/doors/lock")
    suspend fun remoteLockDoors(
        @Header("Authorization") token: String,
        @Path("vin") vin: String
    ): Response<RemoteControlResponse>

    @POST("api/v1/vehicles/{vin}/remote/doors/unlock")
    suspend fun remoteUnlockDoors(
        @Header("Authorization") token: String,
        @Path("vin") vin: String
    ): Response<RemoteControlResponse>

    @POST("api/v1/vehicles/{vin}/remote/lights/hazard")
    suspend fun remoteHazardLights(
        @Header("Authorization") token: String,
        @Path("vin") vin: String
    ): Response<RemoteControlResponse>

    @POST("api/v1/vehicles/{vin}/remote/horn/sound")
    suspend fun remoteHorn(
        @Header("Authorization") token: String,
        @Path("vin") vin: String
    ): Response<RemoteControlResponse>

    @POST("api/v1/vehicles/{vin}/remote/trunk/toggle")
    suspend fun remoteTrunk(
        @Header("Authorization") token: String,
        @Path("vin") vin: String
    ): Response<RemoteControlResponse>

    // --- Diagnostics & OBD-II DTC Endpoints ---

    @POST("api/v1/vehicles/{vin}/diagnostics/scan")
    suspend fun runDiagnosticScan(
        @Header("Authorization") token: String,
        @Path("vin") vin: String
    ): Response<RemoteControlResponse>

    @POST("api/v1/vehicles/{vin}/diagnostics/clear")
    suspend fun clearDtcCodes(
        @Header("Authorization") token: String,
        @Path("vin") vin: String
    ): Response<RemoteControlResponse>

    @GET("api/v1/health")
    suspend fun pingGateway(
        @Header("Authorization") token: String
    ): Response<RemoteControlResponse>

    // --- Legacy / cv/mcs Bridge Direct Route Compatibility ---

    @POST("control/execute")
    suspend fun executeControlCommand(
        @Header("Authorization") token: String,
        @Body request: RemoteControlRequest
    ): Response<RemoteControlResponse>

    @GET("status/{vin}")
    suspend fun getVehicleTelematics(
        @Header("Authorization") token: String,
        @Path("vin") vin: String
    ): Response<CloudTelematicsDto>
}

// ----------------- Retrofit Client Builder -----------------

object ApiClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Redact Authorization headers to prevent token leakage in Logcat
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
