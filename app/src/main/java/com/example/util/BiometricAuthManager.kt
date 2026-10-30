package com.example.util

import android.content.Context
import android.os.Build

data class BiometricSecurityConfig(
    val requireForRemoteStart: Boolean = true,
    val requireForDoorUnlock: Boolean = true,
    val requireForTheftAlarm: Boolean = true,
    val requireForAppLaunch: Boolean = false,
    val userPin: String = "1920"
)

object BiometricAuthManager {
    fun hasBiometricHardware(context: Context): Boolean {
        // Safe check for fingerprint / biometric sensor availability
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val fingerprintManager = context.getSystemService(Context.FINGERPRINT_SERVICE)
                val method = fingerprintManager?.javaClass?.getMethod("isHardwareDetected")
                (method?.invoke(fingerprintManager) as? Boolean) ?: true
            } catch (_: Exception) {
                true
            }
        } else {
            true
        }
    }
}
