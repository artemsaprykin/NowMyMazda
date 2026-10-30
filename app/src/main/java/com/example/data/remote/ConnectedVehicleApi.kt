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

data class RemoteControlRequest(
    @Json(name = "vin") val vin: String,
    @Json(name = "action") val action: String, // START_ENGINE, STOP_ENGINE, LOCK, UNLOCK, FLASH_LIGHTS, SOUND_HORN
    @Json(name = "duration_minutes") val durationMinutes: Int? = 15,
    @Json(name = "climate_temp_f") val climateTempF: Int? = 72,
    @Json(name = "heated_seats") val heatedSeats: Boolean? = true
)

data class RemoteControlResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "command_id") val commandId: String,
    @Json(name = "status") val status: String,
    @Json(name = "message") val message: String
)

data class CloudTelematicsDto(
    @Json(name = "vin") val vin: String,
    @Json(name = "engine_status") val engineStatus: String,
    @Json(name = "door_lock_status") val doorLockStatus: String,
    @Json(name = "fuel_level_percent") val fuelLevelPercent: Int,
    @Json(name = "battery_voltage") val batteryVoltage: Double,
    @Json(name = "battery_soh") val batterySoh: Int,
    @Json(name = "tpms_fl_psi") val tpmsFlPsi: Double,
    @Json(name = "tpms_fr_psi") val tpmsFrPsi: Double,
    @Json(name = "tpms_rl_psi") val tpmsRlPsi: Double,
    @Json(name = "tpms_rr_psi") val tpmsRrPsi: Double,
    @Json(name = "oil_life_percent") val oilLifePercent: Int,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "odometer_miles") val odometerMiles: Int,
    @Json(name = "active_dtc_codes") val dtcCodes: List<String> = emptyList()
)

data class McsAuthRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "region") val region: String = "MNAO"
)

data class McsAuthResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String = "Bearer",
    @Json(name = "expires_in") val expiresIn: Long = 86400,
    @Json(name = "customer_id") val customerId: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "status") val status: String? = "AUTHENTICATED"
)

data class McsVehicleDto(
    @Json(name = "vin") val vin: String,
    @Json(name = "id") val id: Long? = null,
    @Json(name = "model_name") val modelName: String? = null,
    @Json(name = "year") val year: Int? = null,
    @Json(name = "nickname") val nickname: String? = null,
    @Json(name = "connected_services_active") val connectedServicesActive: Boolean = true
)

interface ConnectedVehicleApiService {
    @POST("auth/login")
    suspend fun loginWithMcsCredentials(
        @Body request: McsAuthRequest
    ): Response<McsAuthResponse>

    @GET("vehicles")
    suspend fun getVehiclesList(
        @Header("Authorization") token: String
    ): Response<List<McsVehicleDto>>

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

    @POST("diagnostics/scan/{vin}")
    suspend fun runDiagnosticScan(
        @Header("Authorization") token: String,
        @Path("vin") vin: String
    ): Response<RemoteControlResponse>

    @POST("diagnostics/clear/{vin}")
    suspend fun clearDtcCodes(
        @Header("Authorization") token: String,
        @Path("vin") vin: String
    ): Response<RemoteControlResponse>

    @GET("health")
    suspend fun pingGateway(
        @Header("Authorization") token: String
    ): Response<RemoteControlResponse>
}

object ApiClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
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
