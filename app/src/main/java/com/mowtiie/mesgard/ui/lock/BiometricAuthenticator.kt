package com.mowtiie.mesgard.ui.lock

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

internal enum class AuthAvailability {
    AVAILABLE,
    NOT_SET_UP,
    UNAVAILABLE,
}

internal enum class AuthOutcome {
    SUCCESS,
    CANCELLED,
    FAILED,
}

internal object BiometricAuthenticator {
    fun availability(context: Context): AuthAvailability =
        when (BiometricManager.from(context).canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> AuthAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> AuthAvailability.NOT_SET_UP
            else -> AuthAvailability.UNAVAILABLE
        }

    suspend fun authenticate(activity: FragmentActivity, title: String): AuthOutcome =
        suspendCancellableCoroutine { continuation ->
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    continuation.resume(AuthOutcome.SUCCESS)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    continuation.resume(if (errorCode.isUserDismissal()) AuthOutcome.CANCELLED else AuthOutcome.FAILED)
                }
            }
            val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build()
            prompt.authenticate(info)
            continuation.invokeOnCancellation { prompt.cancelAuthentication() }
        }

    private fun Int.isUserDismissal(): Boolean = this == BiometricPrompt.ERROR_USER_CANCELED ||
        this == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
        this == BiometricPrompt.ERROR_CANCELED
}
