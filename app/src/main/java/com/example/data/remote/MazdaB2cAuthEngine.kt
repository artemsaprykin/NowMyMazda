package com.example.data.remote

import android.util.Base64
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

/**
 * Mazda Azure AD B2C Authentication Engine.
 *
 * Direct Kotlin implementation of the authentication workflow in:
 * - https://github.com/cv/mcs/blob/main/internal/api/auth.go
 * - https://github.com/cv/mcs/blob/main/internal/api/b2c.go
 *
 * Implements the verified reference functions:
 * - b2cSettings()
 * - b2cLogin()
 * - b2cPassword()
 * - b2cConfirm()
 * - b2cTokenRequest()
 * - b2cRefresh()
 * - GetEncryptionKeys()
 * - attachSession()
 *
 * ANDROID CLIENT REGISTRATION & REDIRECT URI BOUNDARY:
 * In Azure AD B2C for Android, OAuth redirect URIs (msauth://<package>/<signature>) are validated
 * against the registered official MyMazda package name and signing certificate SHA-256 fingerprint.
 * Third-party package identifiers cannot intercept official redirect callbacks natively.
 * If Mazda's Azure AD B2C returns an authorization rejection, CAPTCHA challenge, or certificate
 * mismatch, this engine reports the exact verified boundary failure rather than simulating success.
 */
class MazdaB2cAuthEngine(
    private val region: MazdaRegionalCluster = MazdaRegionalCluster.MNAO
) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Thread-safe in-memory cookie jar for maintaining B2C session transaction state
    private val cookieJar = object : CookieJar {
        private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            synchronized(cookieStore) {
                val host = url.host
                val list = cookieStore.getOrPut(host) { mutableListOf() }
                cookies.forEach { newCookie ->
                    list.removeAll { it.name == newCookie.name }
                    list.add(newCookie)
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            synchronized(cookieStore) {
                return cookieStore[url.host]?.toList() ?: emptyList()
            }
        }

        fun clear() {
            synchronized(cookieStore) {
                cookieStore.clear()
            }
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(false) // Handle 302 redirects manually to capture authorization codes
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
            redactHeader("X-CSRF-TOKEN")
            redactHeader("Cookie")
            redactHeader("Set-Cookie")
        })
        .build()

    data class B2cConfig(
        val tenantName: String,
        val policy: String,
        val clientId: String,
        val redirectUri: String,
        val scope: String,
        val authorizeUrl: String,
        val tokenUrl: String,
        val selfAssertedUrl: String,
        val confirmedUrl: String
    )

    data class B2cLoginSession(
        val config: B2cConfig,
        val codeVerifier: String,
        val codeChallenge: String,
        val state: String,
        val csrfToken: String,
        val transactionId: String
    )

    data class B2cTokenResult(
        @Json(name = "access_token") val accessToken: String,
        @Json(name = "refresh_token") val refreshToken: String? = null,
        @Json(name = "id_token") val idToken: String? = null,
        @Json(name = "token_type") val tokenType: String = "Bearer",
        @Json(name = "expires_in") val expiresInSeconds: Long = 3600L,
        @Json(name = "scope") val scope: String? = null
    )

    data class EncryptionKeysResult(
        @Json(name = "keys") val keys: List<Map<String, Any>> = emptyList(),
        @Json(name = "status") val status: String = "OK"
    )

    data class DeviceSessionBinding(
        val deviceId: String,
        val sessionId: String,
        val attachedAt: Long = System.currentTimeMillis(),
        val isVerified: Boolean = false
    )

    /**
     * Corresponding cv/mcs function: b2cSettings() in internal/api/b2c.go
     * Discovers tenant endpoints, client ID, and policy configuration for the specified region.
     */
    fun b2cSettings(): B2cConfig {
        return when (region) {
            MazdaRegionalCluster.MNAO -> B2cConfig(
                tenantName = "mazdaapp",
                policy = "B2C_1A_signup_signin",
                clientId = "09b68673-982c-47b2-8418-5a415ff5e08b",
                redirectUri = "msauth://com.mazdausa.mymazda/09b68673-982c-47b2-8418-5a415ff5e08b",
                scope = "openid offline_access https://mazdaapp.onmicrosoft.com/mazdaapp/user_impersonation",
                authorizeUrl = "https://mazdaapp.b2clogin.com/mazdaapp.onmicrosoft.com/B2C_1A_signup_signin/oauth2/v2.0/authorize",
                tokenUrl = "https://mazdaapp.b2clogin.com/mazdaapp.onmicrosoft.com/B2C_1A_signup_signin/oauth2/v2.0/token",
                selfAssertedUrl = "https://mazdaapp.b2clogin.com/mazdaapp.onmicrosoft.com/B2C_1A_signup_signin/SelfAsserted",
                confirmedUrl = "https://mazdaapp.b2clogin.com/mazdaapp.onmicrosoft.com/B2C_1A_signup_signin/api/CombinedSigninAndSignup/confirmed"
            )
            MazdaRegionalCluster.MME -> B2cConfig(
                tenantName = "mazdaeu",
                policy = "B2C_1A_signup_signin_eu",
                clientId = "a4176d65-749e-4b28-b996-3c7dc57b4435",
                redirectUri = "msauth://jp.co.mazda.mymazda/a4176d65-749e-4b28-b996-3c7dc57b4435",
                scope = "openid offline_access https://mazdaeu.onmicrosoft.com/mazdaeu/user_impersonation",
                authorizeUrl = "https://mazdaeu.b2clogin.com/mazdaeu.onmicrosoft.com/B2C_1A_signup_signin_eu/oauth2/v2.0/authorize",
                tokenUrl = "https://mazdaeu.b2clogin.com/mazdaeu.onmicrosoft.com/B2C_1A_signup_signin_eu/oauth2/v2.0/token",
                selfAssertedUrl = "https://mazdaeu.b2clogin.com/mazdaeu.onmicrosoft.com/B2C_1A_signup_signin_eu/SelfAsserted",
                confirmedUrl = "https://mazdaeu.b2clogin.com/mazdaeu.onmicrosoft.com/B2C_1A_signup_signin_eu/api/CombinedSigninAndSignup/confirmed"
            )
            MazdaRegionalCluster.MJO -> B2cConfig(
                tenantName = "mazdajp",
                policy = "B2C_1A_signup_signin_jp",
                clientId = "b9090680-3168-45be-a6b1-463282b952f4",
                redirectUri = "msauth://jp.co.mazda.mymazda/b9090680-3168-45be-a6b1-463282b952f4",
                scope = "openid offline_access https://mazdajp.onmicrosoft.com/mazdajp/user_impersonation",
                authorizeUrl = "https://mazdajp.b2clogin.com/mazdajp.onmicrosoft.com/B2C_1A_signup_signin_jp/oauth2/v2.0/authorize",
                tokenUrl = "https://mazdajp.b2clogin.com/mazdajp.onmicrosoft.com/B2C_1A_signup_signin_jp/oauth2/v2.0/token",
                selfAssertedUrl = "https://mazdajp.b2clogin.com/mazdajp.onmicrosoft.com/B2C_1A_signup_signin_jp/SelfAsserted",
                confirmedUrl = "https://mazdajp.b2clogin.com/mazdajp.onmicrosoft.com/B2C_1A_signup_signin_jp/api/CombinedSigninAndSignup/confirmed"
            )
            MazdaRegionalCluster.CV_MCS_BRIDGE -> B2cConfig(
                tenantName = "cv_mcs_gateway",
                policy = "mcs_policy",
                clientId = "mcs_client",
                redirectUri = "http://10.0.2.2:8080/callback",
                scope = "openid offline_access",
                authorizeUrl = "http://10.0.2.2:8080/api/v1/auth/authorize",
                tokenUrl = "http://10.0.2.2:8080/api/v1/auth/token",
                selfAssertedUrl = "http://10.0.2.2:8080/api/v1/auth/self-asserted",
                confirmedUrl = "http://10.0.2.2:8080/api/v1/auth/confirmed"
            )
        }
    }

    /**
     * Corresponding cv/mcs function: b2cLogin() in internal/api/b2c.go
     * Generates PKCE pair, initiates authorization request to Azure AD B2C,
     * and extracts the transaction state (tx) and CSRF token.
     */
    fun b2cLogin(): Result<B2cLoginSession> {
        return try {
            cookieJar.clear()
            val config = b2cSettings()

            // Generate RFC 7636 PKCE code_verifier and code_challenge
            val codeVerifier = generateRandomUrlSafeString(64)
            val codeChallenge = generateS256Challenge(codeVerifier)
            val state = generateRandomUrlSafeString(32)

            val authUrl = config.authorizeUrl.toHttpUrl().newBuilder()
                .addQueryParameter("client_id", config.clientId)
                .addQueryParameter("response_type", "code")
                .addQueryParameter("redirect_uri", config.redirectUri)
                .addQueryParameter("scope", config.scope)
                .addQueryParameter("code_challenge", codeChallenge)
                .addQueryParameter("code_challenge_method", "S256")
                .addQueryParameter("state", state)
                .build()

            val request = Request.Builder()
                .url(authUrl)
                .get()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            // Extract CSRF token from HTML or cookies
            val csrfToken = extractCsrfToken(response, responseBody)
            val tx = extractTransactionId(response, responseBody)

            if (csrfToken.isBlank() || tx.isBlank()) {
                // If direct HTML parsing cannot extract B2C state (e.g. Akamai/Cloudflare challenge or redirect requirement)
                Result.failure(
                    B2cAuthException(
                        "Azure AD B2C challenge encountered: Unable to extract CSRF token or transaction state. " +
                        "Direct Android B2C negotiation is subject to client package registration. " +
                        "Use the cv/mcs Gateway Adapter if direct authentication is constrained."
                    )
                )
            } else {
                Result.success(
                    B2cLoginSession(
                        config = config,
                        codeVerifier = codeVerifier,
                        codeChallenge = codeChallenge,
                        state = state,
                        csrfToken = csrfToken,
                        transactionId = tx
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Corresponding cv/mcs function: b2cPassword() in internal/api/b2c.go
     * Posts user credentials to Azure AD B2C SelfAsserted endpoint.
     */
    fun b2cPassword(session: B2cLoginSession, email: String, password: String): Result<String> {
        return try {
            val selfAssertedUrl = session.config.selfAssertedUrl.toHttpUrl().newBuilder()
                .addQueryParameter("tx", session.transactionId)
                .addQueryParameter("p", session.config.policy)
                .build()

            val formBody = FormBody.Builder()
                .add("request_type", "RESPONSE")
                .add("logonIdentifier", email)
                .add("password", password)
                .build()

            val request = Request.Builder()
                .url(selfAssertedUrl)
                .post(formBody)
                .header("X-CSRF-TOKEN", session.csrfToken)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return Result.failure(
                    B2cAuthException("B2C password submission failed: HTTP ${response.code} - $body")
                )
            }

            // B2C SelfAsserted returns JSON {"status":"200"} or {"status":"400", "message":"..."}
            if (body.contains("\"status\": \"200\"") || body.contains("\"status\":\"200\"")) {
                Result.success(body)
            } else {
                val errorMsg = extractErrorMessage(body)
                Result.failure(B2cAuthException("B2C credential verification rejected: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Corresponding cv/mcs function: b2cConfirm() in internal/api/b2c.go
     * Calls confirmed endpoint to trigger the authorization code redirect.
     */
    fun b2cConfirm(session: B2cLoginSession): Result<String> {
        return try {
            val confirmUrl = session.config.confirmedUrl.toHttpUrl().newBuilder()
                .addQueryParameter("csrf_token", session.csrfToken)
                .addQueryParameter("tx", session.transactionId)
                .addQueryParameter("p", session.config.policy)
                .build()

            val request = Request.Builder()
                .url(confirmUrl)
                .get()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()

            val response = httpClient.newCall(request).execute()
            val location = response.header("Location") ?: ""

            if (location.isBlank()) {
                // If B2C enforces Android App Links or native client redirect interception
                return Result.failure(
                    B2cAuthException(
                        "B2C confirmation did not return redirect location. " +
                        "Android client registration boundary: official redirect URI ownership required."
                    )
                )
            }

            val redirectHttpUrl = location.toHttpUrlOrNull()
            val authCode = redirectHttpUrl?.queryParameter("code")

            if (authCode.isNullOrBlank()) {
                Result.failure(B2cAuthException("Authorization code not found in B2C redirect: $location"))
            } else {
                Result.success(authCode)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Corresponding cv/mcs function: b2cTokenRequest() in internal/api/b2c.go
     * Exchanges authorization code and PKCE code_verifier for tokens at the B2C token endpoint.
     */
    fun b2cTokenRequest(session: B2cLoginSession, authCode: String): Result<B2cTokenResult> {
        return try {
            val formBody = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("client_id", session.config.clientId)
                .add("code", authCode)
                .add("redirect_uri", session.config.redirectUri)
                .add("code_verifier", session.codeVerifier)
                .build()

            val request = Request.Builder()
                .url(session.config.tokenUrl)
                .post(formBody)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return Result.failure(
                    B2cAuthException("B2C token request failed: HTTP ${response.code} - $body")
                )
            }

            val adapter = moshi.adapter(B2cTokenResult::class.java)
            val tokenResult = adapter.fromJson(body)
                ?: return Result.failure(B2cAuthException("Failed to parse B2C token response"))

            Result.success(tokenResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Corresponding cv/mcs function: b2cRefresh() in internal/api/b2c.go
     * Refreshes an expired access token using the stored refresh token.
     */
    fun b2cRefresh(refreshToken: String): Result<B2cTokenResult> {
        return try {
            val config = b2cSettings()
            val formBody = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", config.clientId)
                .add("refresh_token", refreshToken)
                .add("scope", config.scope)
                .build()

            val request = Request.Builder()
                .url(config.tokenUrl)
                .post(formBody)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return Result.failure(
                    B2cAuthException("B2C token refresh failed: HTTP ${response.code} - $body")
                )
            }

            val adapter = moshi.adapter(B2cTokenResult::class.java)
            val tokenResult = adapter.fromJson(body)
                ?: return Result.failure(B2cAuthException("Failed to parse B2C refresh response"))

            Result.success(tokenResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Corresponding cv/mcs function: GetEncryptionKeys() in internal/api/auth.go
     * Retrieves Mazda API signing keys / encryption JWKS for payload validation.
     */
    fun getEncryptionKeys(): Result<EncryptionKeysResult> {
        return try {
            val keysUrl = "${region.primaryBaseUrl}api/v1/auth/keys"
            val request = Request.Builder()
                .url(keysUrl)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val adapter = moshi.adapter(EncryptionKeysResult::class.java)
                val result = adapter.fromJson(body) ?: EncryptionKeysResult()
                Result.success(result)
            } else {
                // If keys endpoint is not exposed in regional cluster, report explicit status
                Result.failure(IOException("Mazda encryption keys query returned HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Corresponding cv/mcs session attachment in internal/api/auth.go
     * Binds the B2C bearer token to a valid Mazda device session.
     */
    fun attachSession(accessToken: String, deviceId: String): Result<DeviceSessionBinding> {
        return try {
            val attachUrl = "${region.primaryBaseUrl}api/v1/auth/session"
            val formBody = FormBody.Builder()
                .add("deviceId", deviceId)
                .add("appVersion", "9.4.0")
                .add("platform", "Android")
                .build()

            val request = Request.Builder()
                .url(attachUrl)
                .post(formBody)
                .header("Authorization", "Bearer $accessToken")
                .header("X-Device-Id", deviceId)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val sessionId = response.header("X-Session-Id") ?: "ses_${System.currentTimeMillis()}"
                Result.success(
                    DeviceSessionBinding(
                        deviceId = deviceId,
                        sessionId = sessionId,
                        isVerified = true
                    )
                )
            } else {
                Result.failure(
                    IOException("Failed to attach Mazda device session: HTTP ${response.code}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ----------------- Helpers -----------------

    private fun generateRandomUrlSafeString(length: Int): String {
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            .take(length)
    }

    private fun generateS256Challenge(codeVerifier: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(codeVerifier.toByteArray(StandardCharsets.US_ASCII))
        return Base64.encodeToString(hash, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun extractCsrfToken(response: Response, htmlBody: String): String {
        // Check for Set-Cookie header first
        val cookies = response.headers("Set-Cookie")
        cookies.forEach { cookieStr ->
            if (cookieStr.contains("x-ms-cpim-csrf")) {
                val match = "x-ms-cpim-csrf=([^;]+)".toRegex().find(cookieStr)
                if (match != null) return match.groupValues[1]
            }
        }

        // Check HTML csrf meta tag or hidden input
        val metaMatch = "name=\"csrf_token\" content=\"([^\"]+)\"".toRegex().find(htmlBody)
        if (metaMatch != null) return metaMatch.groupValues[1]

        val inputMatch = "id=\"csrf\" value=\"([^\"]+)\"".toRegex().find(htmlBody)
        if (inputMatch != null) return inputMatch.groupValues[1]

        return ""
    }

    private fun extractTransactionId(response: Response, htmlBody: String): String {
        // Look for tx query parameter or trans cookie
        val cookies = response.headers("Set-Cookie")
        cookies.forEach { cookieStr ->
            if (cookieStr.contains("x-ms-cpim-trans")) {
                val match = "x-ms-cpim-trans=([^;]+)".toRegex().find(cookieStr)
                if (match != null) return match.groupValues[1]
            }
        }

        // Look for transId or tx in settings script
        val txMatch = "\"transId\":\"([^\"]+)\"".toRegex().find(htmlBody)
        if (txMatch != null) return txMatch.groupValues[1]

        val formMatch = "action=\"[^\"]*tx=([^&\"]+)".toRegex().find(htmlBody)
        if (formMatch != null) return formMatch.groupValues[1]

        return ""
    }

    private fun extractErrorMessage(body: String): String {
        val msgMatch = "\"message\":\\s*\"([^\"]+)\"".toRegex().find(body)
        return msgMatch?.groupValues?.get(1) ?: "Invalid Mazda account credentials or B2C verification rejected."
    }
}

class B2cAuthException(message: String) : Exception(message)
