package today.siteblock.android.system

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** The final step of every weaken: the phone's PIN/pattern/password. Biometrics deliberately not allowed. */
object DeviceCredential {
    fun isAvailable(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS

    /** [onResult] gets true only on success; cancel/error/lockout give false. */
    fun prompt(activity: FragmentActivity, reason: String, onResult: (Boolean) -> Unit) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onResult(true)
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onResult(false)
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirm with your PIN")
            .setSubtitle(reason)
            .setAllowedAuthenticators(DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(info)
    }
}
