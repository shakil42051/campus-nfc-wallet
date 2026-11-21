package com.example.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricUtils {

    private const val STRONG_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG

    /**
     * Checks whether the device hardware and system support Class 3 (BIOMETRIC_STRONG) authentication.
     */
    fun checkBiometricSupport(context: Context): Pair<Boolean, String> {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(STRONG_AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Pair(true, "Supported")
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Pair(false, "This device does not support strong biometric authentication.")
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Pair(false, "Biometric hardware is currently unavailable.")
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Pair(false, "No strong biometrics (fingerprint/3D face) enrolled on this device.")
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> Pair(false, "A system security update is required before biometrics can be used.")
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> Pair(false, "Biometric status could not be determined.")
            else -> Pair(false, "This device does not support strong biometric authentication.")
        }
    }

    /**
     * Displays the official native system BiometricPrompt dialog using BIOMETRIC_STRONG.
     */
    fun showSystemBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Verify your identity",
        subtitle: String = "Touch the fingerprint sensor",
        negativeButtonText: String = "Use PIN / Password",
        onSuccess: () -> Unit,
        onFailedAttempt: () -> Unit = {},
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    onCancel()
                } else {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailedAttempt()
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(STRONG_AUTHENTICATORS)
            .build()

        try {
            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Failed to launch biometric prompt")
        }
    }
}
