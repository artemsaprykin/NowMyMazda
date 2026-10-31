package com.example.util

import android.content.Context
import android.os.Build

data class BiometricSecurityConfig(
    val requireForRemoteStart: Boolean = true,
    val requireForDoorUnlock: Boolean = true,
    val requireForTheftAlarm: Boolean = true,
    val requireForAppLaunch: Boolean = false,
    val hasConfiguredPin: Boolean = false
)

object BiometricAuthManager {
    fun hasBiometricHardware(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val fingerprintManager = context.getSystemService(Context.FINGERPRINT_SERVICE)
                val method = fingerprintManager?.javaClass?.getMethod("isHardwareDetected")
                (method?.invoke(fingerprintManager) as? Boolean) ?: false
            } catch (_: Exception) {
                false
            }
        } else {
            false
        }
    }

    /**
     * Launches the real Android system biometric authentication dialog (Fingerprint/Face).
     */
    fun authenticateWithSystemBiometrics(
        activity: android.app.Activity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val cancellationSignal = android.os.CancellationSignal()
                val executor = activity.mainExecutor
                val callback = object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        activity.runOnUiThread { onSuccess() }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        activity.runOnUiThread {
                            onError(errString?.toString() ?: "Biometric verification cancelled")
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        activity.runOnUiThread {
                            onError("Fingerprint or Face biometric not recognized.")
                        }
                    }
                }

                val prompt = android.hardware.biometrics.BiometricPrompt.Builder(activity)
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setNegativeButton("Enter Security PIN", executor) { _, _ ->
                        activity.runOnUiThread { onError("USER_SELECTED_PIN") }
                    }
                    .build()

                prompt.authenticate(cancellationSignal, executor, callback)
            } catch (e: Exception) {
                onError(e.message ?: "Biometric hardware unavailable on this device")
            }
        } else {
            onError("BiometricPrompt requires Android 9.0+")
        }
    }
}
