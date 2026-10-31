package com.example.data.storage

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore-backed AES-256-GCM encryption storage for sensitive Mazda API session tokens.
 * Plaintext tokens are never stored in SharedPreferences, never exposed via UI StateFlows, and never logged.
 *
 * Implements strict fail-closed security:
 * - No plain Base64 fallback on encryption or decryption.
 * - If Android Keystore encryption fails in production, an exception is thrown.
 */
class SecureTokenStorage(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class StoredSession(
        val accessToken: String,
        val refreshToken: String?,
        val idToken: String?,
        val tokenExpiry: Long,
        val customerId: String,
        val region: String,
        val deviceId: String
    )

    companion object {
        private const val PREFS_NAME = "secure_tokens"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "NowMyMazda_Master_Key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_SIZE = 12

        private const val KEY_ACCESS_TOKEN_ENC = "access_token_enc"
        private const val KEY_REFRESH_TOKEN_ENC = "refresh_token_enc"
        private const val KEY_ID_TOKEN_ENC = "id_token_enc"
        private const val KEY_EXPIRY = "token_expiry"
        private const val KEY_CUSTOMER_ID = "customer_id"
        private const val KEY_REGION = "session_region"
        private const val KEY_DEVICE_ID = "device_id"

        @Volatile
        private var cachedTestFallbackKey: SecretKey? = null

        private fun isTestRunner(): Boolean {
            return try {
                Class.forName("org.robolectric.Robolectric")
                true
            } catch (_: ClassNotFoundException) {
                false
            }
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                (keyStore.getEntry(MASTER_KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
            } else {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val spec = KeyGenParameterSpec.Builder(
                    MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            if (isTestRunner()) {
                cachedTestFallbackKey ?: synchronized(SecureTokenStorage::class.java) {
                    cachedTestFallbackKey ?: KeyGenerator.getInstance("AES").apply { init(256) }.generateKey().also {
                        cachedTestFallbackKey = it
                    }
                }
            } else {
                throw SecurityException("Hardware Android Keystore is unavailable for secure token storage: ${e.message}", e)
            }
        }
    }

    private fun encrypt(plainText: String): String {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv ?: throw SecurityException("Cipher initialization failed: null IV")
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encryptedBase64: String): String? {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.DEFAULT)
            if (combined.size < IV_SIZE) return null
            val iv = ByteArray(IV_SIZE)
            val cipherText = ByteArray(combined.size - IV_SIZE)
            System.arraycopy(combined, 0, iv, 0, IV_SIZE)
            System.arraycopy(combined, IV_SIZE, cipherText, 0, cipherText.size)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            // Strict fail-closed: Never fall back to raw Base64 decoding
            null
        }
    }

    fun saveSession(
        accessToken: String,
        refreshToken: String?,
        idToken: String? = null,
        tokenExpiry: Long,
        customerId: String,
        region: String,
        deviceId: String = ""
    ) {
        val encryptedAccess = encrypt(accessToken)
        val encryptedRefresh = refreshToken?.let { encrypt(it) }
        val encryptedId = idToken?.let { encrypt(it) }

        prefs.edit()
            .putString(KEY_ACCESS_TOKEN_ENC, encryptedAccess)
            .putString(KEY_REFRESH_TOKEN_ENC, encryptedRefresh)
            .putString(KEY_ID_TOKEN_ENC, encryptedId)
            .putLong(KEY_EXPIRY, tokenExpiry)
            .putString(KEY_CUSTOMER_ID, customerId)
            .putString(KEY_REGION, region)
            .putString(KEY_DEVICE_ID, deviceId)
            .apply()
    }

    fun loadSession(): StoredSession? {
        val encAccess = prefs.getString(KEY_ACCESS_TOKEN_ENC, null) ?: return null
        val accessToken = decrypt(encAccess) ?: return null
        val encRefresh = prefs.getString(KEY_REFRESH_TOKEN_ENC, null)
        val refreshToken = encRefresh?.let { decrypt(it) }
        val encId = prefs.getString(KEY_ID_TOKEN_ENC, null)
        val idToken = encId?.let { decrypt(it) }
        val expiry = prefs.getLong(KEY_EXPIRY, 0L)
        val customerId = prefs.getString(KEY_CUSTOMER_ID, "") ?: ""
        val region = prefs.getString(KEY_REGION, "MNAO") ?: "MNAO"
        val deviceId = prefs.getString(KEY_DEVICE_ID, "") ?: ""

        return StoredSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            idToken = idToken,
            tokenExpiry = expiry,
            customerId = customerId,
            region = region,
            deviceId = deviceId
        )
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun hasValidSession(): Boolean {
        val session = loadSession() ?: return false
        if (session.accessToken.isBlank()) return false
        // Check if token has not expired (with 60-second grace window)
        return if (session.tokenExpiry > 0L) {
            System.currentTimeMillis() < (session.tokenExpiry - 60_000L)
        } else {
            false
        }
    }
}
