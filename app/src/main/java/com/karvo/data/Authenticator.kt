package com.karvo.data

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine

/** Asks the user to prove it's them (fingerprint, face or the phone's PIN) before earnings are shown. */
interface Authenticator {
    /** False when the phone has no screen lock, so there is nothing to ask for. */
    fun isAvailable(): Boolean

    /** True on success; false when cancelled, failed or no screen is there to show the prompt. */
    suspend fun authenticate(title: String): Boolean

    companion object {
        /** For phones and tests without a screen lock: earnings show as before 2.1. */
        val None: Authenticator = object : Authenticator {
            override fun isAvailable() = false
            override suspend fun authenticate(title: String) = false
        }
    }
}

/** Earnings may be shown when protection is off, the phone has no screen lock, or the user passes the prompt. */
suspend fun mayRevealEarnings(settings: SettingsRepository, authenticator: Authenticator, promptTitle: String): Boolean =
    !settings.biometricReveal.first() || !authenticator.isAvailable() || authenticator.authenticate(promptTitle)

private const val ALLOWED = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

/** Shows the system prompt on the activity currently attached by [com.karvo.MainActivity]. */
class BiometricAuthenticator(context: Context) : Authenticator {
    private val appContext = context.applicationContext
    private var activity: FragmentActivity? = null

    fun attach(activity: FragmentActivity) {
        this.activity = activity
    }

    fun detach(activity: FragmentActivity) {
        if (this.activity === activity) this.activity = null
    }

    override fun isAvailable(): Boolean =
        BiometricManager.from(appContext).canAuthenticate(ALLOWED) == BiometricManager.BIOMETRIC_SUCCESS

    override suspend fun authenticate(title: String): Boolean {
        val host = activity ?: return false
        return suspendCancellableCoroutine { cont ->
            val prompt = BiometricPrompt(
                host,
                ContextCompat.getMainExecutor(host),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (cont.isActive) cont.resume(true)
                    }

                    // onAuthenticationFailed is a single rejected attempt; the prompt stays open for a retry.
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (cont.isActive) cont.resume(false)
                    }
                },
            )
            prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(title).setAllowedAuthenticators(ALLOWED).build())
            cont.invokeOnCancellation { prompt.cancelAuthentication() }
        }
    }
}
