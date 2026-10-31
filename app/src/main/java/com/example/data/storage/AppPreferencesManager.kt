package com.example.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nowmymazda_settings")

data class PersistedSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val requireBiometricForRemoteStart: Boolean = true,
    val requireBiometricForDoorUnlock: Boolean = true,
    val requireBiometricForTheftAlarm: Boolean = true,
    val requireBiometricForAppLaunch: Boolean = false,
    val hasCustomPin: Boolean = false,
    val accountEmail: String = "",
    val accountRegionCode: String = "MNAO",
    val customBridgeUrl: String = "",
    val selectedVin: String = "",
    val selectedNickname: String = "",
    val remoteStartDurationMinutes: Int = 15,
    val remoteStartTargetTempF: Int = 72,
    val geofenceEnabled: Boolean = false,
    val geofenceRadiusMeters: Int = 300,
    val geofenceSafeZoneName: String = "Safe Zone",
    val geofenceCenterLat: Double = 0.0,
    val geofenceCenterLng: Double = 0.0
)

class AppPreferencesManager(private val context: Context) {

    companion object {
        private val KEY_THEME = stringPreferencesKey("app_theme")
        private val KEY_BIO_REMOTE_START = booleanPreferencesKey("bio_remote_start")
        private val KEY_BIO_DOOR_UNLOCK = booleanPreferencesKey("bio_door_unlock")
        private val KEY_BIO_THEFT_ALARM = booleanPreferencesKey("bio_theft_alarm")
        private val KEY_BIO_APP_LAUNCH = booleanPreferencesKey("bio_app_launch")
        private val KEY_PIN_HASH = stringPreferencesKey("user_pin_hash")
        private val KEY_PIN_SALT = stringPreferencesKey("user_pin_salt")

        private val KEY_ACCOUNT_EMAIL = stringPreferencesKey("account_email")
        private val KEY_ACCOUNT_REGION = stringPreferencesKey("account_region")
        private val KEY_CUSTOM_BRIDGE_URL = stringPreferencesKey("custom_bridge_url")
        private val KEY_SELECTED_VIN = stringPreferencesKey("selected_vin")
        private val KEY_SELECTED_NICKNAME = stringPreferencesKey("selected_nickname")

        private val KEY_REMOTE_START_DURATION = intPreferencesKey("remote_start_duration")
        private val KEY_REMOTE_START_TEMP = intPreferencesKey("remote_start_temp")

        private val KEY_GEOFENCE_ENABLED = booleanPreferencesKey("geofence_enabled")
        private val KEY_GEOFENCE_RADIUS = intPreferencesKey("geofence_radius")
        private val KEY_GEOFENCE_NAME = stringPreferencesKey("geofence_name")
        private val KEY_GEOFENCE_LAT = doublePreferencesKey("geofence_lat")
        private val KEY_GEOFENCE_LNG = doublePreferencesKey("geofence_lng")
    }

    val settingsFlow: Flow<PersistedSettings> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[KEY_THEME] ?: ThemeMode.DARK.name
        val theme = try {
            ThemeMode.valueOf(themeStr)
        } catch (_: Exception) {
            ThemeMode.DARK
        }

        PersistedSettings(
            themeMode = theme,
            requireBiometricForRemoteStart = prefs[KEY_BIO_REMOTE_START] ?: true,
            requireBiometricForDoorUnlock = prefs[KEY_BIO_DOOR_UNLOCK] ?: true,
            requireBiometricForTheftAlarm = prefs[KEY_BIO_THEFT_ALARM] ?: true,
            requireBiometricForAppLaunch = prefs[KEY_BIO_APP_LAUNCH] ?: false,
            hasCustomPin = !prefs[KEY_PIN_HASH].isNullOrBlank(),
            accountEmail = prefs[KEY_ACCOUNT_EMAIL] ?: "",
            accountRegionCode = prefs[KEY_ACCOUNT_REGION] ?: "MNAO",
            customBridgeUrl = prefs[KEY_CUSTOM_BRIDGE_URL] ?: "",
            selectedVin = prefs[KEY_SELECTED_VIN] ?: "",
            selectedNickname = prefs[KEY_SELECTED_NICKNAME] ?: "",
            remoteStartDurationMinutes = prefs[KEY_REMOTE_START_DURATION] ?: 15,
            remoteStartTargetTempF = prefs[KEY_REMOTE_START_TEMP] ?: 72,
            geofenceEnabled = prefs[KEY_GEOFENCE_ENABLED] ?: false,
            geofenceRadiusMeters = prefs[KEY_GEOFENCE_RADIUS] ?: 300,
            geofenceSafeZoneName = prefs[KEY_GEOFENCE_NAME] ?: "Safe Zone",
            geofenceCenterLat = prefs[KEY_GEOFENCE_LAT] ?: 0.0,
            geofenceCenterLng = prefs[KEY_GEOFENCE_LNG] ?: 0.0
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = mode.name
        }
    }

    suspend fun setBiometricPreferences(
        forRemoteStart: Boolean,
        forDoorUnlock: Boolean,
        forTheftAlarm: Boolean,
        forAppLaunch: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BIO_REMOTE_START] = forRemoteStart
            prefs[KEY_BIO_DOOR_UNLOCK] = forDoorUnlock
            prefs[KEY_BIO_THEFT_ALARM] = forTheftAlarm
            prefs[KEY_BIO_APP_LAUNCH] = forAppLaunch
        }
    }

    suspend fun setUserPin(newPin: String) {
        val salt = UUID.randomUUID().toString()
        val hash = hashPin(newPin, salt)
        context.dataStore.edit { prefs ->
            prefs[KEY_PIN_SALT] = salt
            prefs[KEY_PIN_HASH] = hash
        }
    }

    suspend fun verifyPin(candidatePin: String): Boolean {
        var isValid = false
        context.dataStore.edit { prefs ->
            val hash = prefs[KEY_PIN_HASH]
            val salt = prefs[KEY_PIN_SALT]
            if (hash.isNullOrBlank() || salt.isNullOrBlank()) {
                // If no PIN is set yet, any 4-digit PIN is not pre-validated (requires setting one)
                isValid = false
            } else {
                val candidateHash = hashPin(candidatePin, salt)
                isValid = (candidateHash == hash)
            }
        }
        return isValid
    }

    suspend fun hasConfiguredPin(): Boolean {
        var hasPin = false
        context.dataStore.edit { prefs ->
            hasPin = !prefs[KEY_PIN_HASH].isNullOrBlank()
        }
        return hasPin
    }

    suspend fun saveAccountSettings(
        email: String,
        regionCode: String,
        customBridgeUrl: String? = null
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCOUNT_EMAIL] = email
            prefs[KEY_ACCOUNT_REGION] = regionCode
            if (customBridgeUrl != null) {
                prefs[KEY_CUSTOM_BRIDGE_URL] = customBridgeUrl
            }
        }
    }

    suspend fun saveSelectedVehicle(vin: String, nickname: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_VIN] = vin
            prefs[KEY_SELECTED_NICKNAME] = nickname
        }
    }

    suspend fun saveRemoteStartPreferences(durationMinutes: Int, targetTempF: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REMOTE_START_DURATION] = durationMinutes
            prefs[KEY_REMOTE_START_TEMP] = targetTempF
        }
    }

    suspend fun saveGeofenceSettings(
        enabled: Boolean,
        radiusMeters: Int,
        name: String,
        lat: Double,
        lng: Double
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GEOFENCE_ENABLED] = enabled
            prefs[KEY_GEOFENCE_RADIUS] = radiusMeters
            prefs[KEY_GEOFENCE_NAME] = name
            prefs[KEY_GEOFENCE_LAT] = lat
            prefs[KEY_GEOFENCE_LNG] = lng
        }
    }

    suspend fun clearAccountAndVehicle() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ACCOUNT_EMAIL)
            prefs.remove(KEY_SELECTED_VIN)
            prefs.remove(KEY_SELECTED_NICKNAME)
        }
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val combined = "$salt:$pin"
        val bytes = digest.digest(combined.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
