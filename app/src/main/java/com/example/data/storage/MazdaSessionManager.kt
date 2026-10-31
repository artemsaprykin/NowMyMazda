package com.example.data.storage

import com.example.data.remote.MazdaB2cAuthEngine
import com.example.data.remote.MazdaRegionalCluster
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages the complete lifecycle of Mazda Connected Services authentication tokens.
 *
 * Implements:
 * - Secure persistence of access, refresh, and id tokens via Android Keystore.
 * - Accurate expiration tracking based on backend-provided expires_in.
 * - Automatic token refresh via Azure AD B2C with mutex protection to prevent concurrent refresh races.
 * - Refresh-token rotation.
 * - Transition to explicit ReauthenticationRequired when refresh fails (never silently attempts full login).
 * - Avoids repeated logins to prevent invalidating official MyMazda mobile sessions.
 */
class MazdaSessionManager(
    private val secureStorage: SecureTokenStorage
) {

    sealed class SessionStatus {
        object Disconnected : SessionStatus()
        data class Authenticated(val validUntilTimestamp: Long, val email: String) : SessionStatus()
        data class Expired(val expiredAtTimestamp: Long, val canRefresh: Boolean) : SessionStatus()
        data class ReauthenticationRequired(val reason: String) : SessionStatus()
    }

    private val _sessionStatus = MutableStateFlow<SessionStatus>(SessionStatus.Disconnected)
    val sessionStatus: StateFlow<SessionStatus> = _sessionStatus.asStateFlow()

    private val refreshMutex = Mutex()

    init {
        restoreSessionState()
    }

    private fun restoreSessionState() {
        val stored = secureStorage.loadSession()
        if (stored != null && stored.accessToken.isNotBlank()) {
            val now = System.currentTimeMillis()
            if (stored.tokenExpiry > 0L && now >= stored.tokenExpiry) {
                _sessionStatus.value = SessionStatus.Expired(
                    expiredAtTimestamp = stored.tokenExpiry,
                    canRefresh = !stored.refreshToken.isNullOrBlank()
                )
            } else {
                _sessionStatus.value = SessionStatus.Authenticated(
                    validUntilTimestamp = stored.tokenExpiry,
                    email = stored.customerId // or email identifier stored in customerId
                )
            }
        } else {
            _sessionStatus.value = SessionStatus.Disconnected
        }
    }

    fun hasValidSession(): Boolean {
        val stored = secureStorage.loadSession() ?: return false
        if (stored.accessToken.isBlank()) return false
        val now = System.currentTimeMillis()
        // 120-second safety margin before expiry
        return stored.tokenExpiry > 0L && now < (stored.tokenExpiry - 120_000L)
    }

    fun getStoredSession(): SecureTokenStorage.StoredSession? {
        return secureStorage.loadSession()
    }

    fun saveAuthenticatedSession(
        accessToken: String,
        refreshToken: String?,
        idToken: String?,
        expiresInSeconds: Long,
        customerId: String,
        region: String,
        deviceId: String = ""
    ) {
        val now = System.currentTimeMillis()
        val expiryTimestamp = now + (expiresInSeconds * 1000L)

        secureStorage.saveSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            idToken = idToken,
            tokenExpiry = expiryTimestamp,
            customerId = customerId,
            region = region,
            deviceId = deviceId
        )

        _sessionStatus.value = SessionStatus.Authenticated(
            validUntilTimestamp = expiryTimestamp,
            email = customerId
        )
    }

    /**
     * Retrieves a valid access token, performing rotation if expired.
     * Returns null if token is expired and refresh fails, requiring user reauthentication.
     */
    suspend fun getValidAccessToken(
        b2cAuthEngine: MazdaB2cAuthEngine
    ): String? {
        val stored = secureStorage.loadSession() ?: return null
        val now = System.currentTimeMillis()

        // If current token is still valid with at least 120s remaining, return it
        if (stored.tokenExpiry > 0L && now < (stored.tokenExpiry - 120_000L)) {
            return stored.accessToken
        }

        // Token is expired or expiring soon; refresh via B2C
        return refreshMutex.withLock {
            // Re-check inside lock
            val current = secureStorage.loadSession() ?: return null
            if (current.tokenExpiry > 0L && System.currentTimeMillis() < (current.tokenExpiry - 120_000L)) {
                return current.accessToken
            }

            val refreshToken = current.refreshToken
            if (refreshToken.isNullOrBlank()) {
                _sessionStatus.value = SessionStatus.ReauthenticationRequired("No refresh token available.")
                return null
            }

            val refreshResult = b2cAuthEngine.b2cRefresh(refreshToken)
            if (refreshResult.isSuccess) {
                val tokenResult = refreshResult.getOrThrow()
                saveAuthenticatedSession(
                    accessToken = tokenResult.accessToken,
                    refreshToken = tokenResult.refreshToken ?: current.refreshToken,
                    idToken = tokenResult.idToken ?: current.idToken,
                    expiresInSeconds = tokenResult.expiresInSeconds,
                    customerId = current.customerId,
                    region = current.region,
                    deviceId = current.deviceId
                )
                tokenResult.accessToken
            } else {
                val error = refreshResult.exceptionOrNull()?.message ?: "Token refresh failed"
                _sessionStatus.value = SessionStatus.ReauthenticationRequired("Session expired: $error")
                null
            }
        }
    }

    fun signOut() {
        secureStorage.clearSession()
        _sessionStatus.value = SessionStatus.Disconnected
    }
}
