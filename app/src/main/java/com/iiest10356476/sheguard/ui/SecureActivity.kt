package com.iiest10356476.sheguard.ui

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.iiest10356476.sheguard.R

class SecureActivity :  BaseActivity() {

    companion object {
        private const val TAG = "SecureActivity" // Define our own TAG
        private const val REQUEST_CODE_CONFIRM_DEVICE_CREDENTIAL = 1234
        const val EXTRA_TARGET_ACTIVITY = "target_activity"
        const val TARGET_MAIN_VAULT = "main_vault"
        const val TARGET_VIEW_ALL = "view_all"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // CRITICAL: Set the layout first
        setContentView(R.layout.activity_secure)

        Log.d(TAG, "SecureActivity created")

        // Add a small delay to ensure UI is ready, then show authentication
        findViewById<android.view.View>(android.R.id.content).postDelayed({
            showDeviceCredentialPrompt()
        }, 500)
    }

    private fun showDeviceCredentialPrompt() {
        Log.d(TAG, "Starting device credential prompt")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val executor = ContextCompat.getMainExecutor(this)
                val biometricPrompt = BiometricPrompt(
                    this,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            Log.d(TAG, "Authentication succeeded")
                            openTargetActivity()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            Log.e(TAG, "Authentication error: $errorCode - $errString")

                            when (errorCode) {
                                BiometricPrompt.ERROR_USER_CANCELED,
                                BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                                    Log.d(TAG, "User canceled authentication")
                                }
                                BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> {
                                    Log.w(TAG, "No device credential set")
                                    Toast.makeText(this@SecureActivity, "Please set up a screen lock", Toast.LENGTH_LONG).show()
                                }
                            }
                            finish()
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            Log.w(TAG, "Authentication failed")
                            // Don't finish here, let user try again
                        }
                    }
                )

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Vault")
                    .setSubtitle("Enter your phone PIN, pattern, or password")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()

                biometricPrompt.authenticate(promptInfo)

            } catch (e: Exception) {
                Log.e(TAG, "Error showing biometric prompt", e)
                showLegacyDeviceCredentialPrompt()
            }

        } else {
            showLegacyDeviceCredentialPrompt()
        }
    }

    private fun showLegacyDeviceCredentialPrompt() {
        Log.d(TAG, "Using legacy device credential prompt")

        try {
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager

            if (!keyguardManager.isKeyguardSecure) {
                Log.w(TAG, "Device is not secured with a lock screen")
                Toast.makeText(this, "Please set up a screen lock first", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                "Unlock Vault",
                "Enter your phone PIN, pattern, or password"
            )

            if (intent != null) {
                @Suppress("DEPRECATION")
                startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIAL)
            } else {
                Log.w(TAG, "Cannot create device credential intent")
                Toast.makeText(this, "Authentication not available", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error with legacy authentication", e)
            Toast.makeText(this, "Authentication failed: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun openTargetActivity() {
        val targetActivity = intent.getStringExtra(EXTRA_TARGET_ACTIVITY)
        Log.d(TAG, "Opening target activity: $targetActivity")

        val targetIntent = when (targetActivity) {
            TARGET_VIEW_ALL -> {
                Log.d(TAG, "Navigating to SecureVaultViewAll")
                Intent(this, SecureVaultViewAll::class.java)
            }
            else -> {
                Log.d(TAG, "Navigating to SecureVault (default)")
                Intent(this, SecureVault::class.java)
            }
        }

        try {
            startActivity(targetIntent)
            Log.d(TAG, "Successfully started target activity")
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting target activity", e)
            Toast.makeText(this, "Error opening vault: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")

        if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIAL) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Device credential confirmed successfully")
                openTargetActivity()
            } else {
                Log.d(TAG, "Device credential confirmation failed or cancelled")
                finish()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Log.d(TAG, "Back pressed - finishing activity")
        finish()
    }
}