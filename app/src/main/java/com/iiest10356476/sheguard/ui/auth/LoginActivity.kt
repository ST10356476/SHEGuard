package com.iiest10356476.sheguard.ui.auth

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.ui.DashboardActivity
import com.iiest10356476.sheguard.viewmodel.AuthViewModel

class LoginActivity : AppCompatActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    // UI Components
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var signInButton: Button
    private lateinit var forgotPasswordLink: TextView
    private lateinit var signUpLink: TextView
    private lateinit var biometricButton: LinearLayout

    // Biometric Components
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupBiometricAuth()
        setupObservers()
        setupClickListeners()
        checkBiometricAvailability()

        // Handle pre-filled email from registration
        handleIntentExtras()
    }

    private fun initViews() {
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        signInButton = findViewById(R.id.sign_in_button)
        forgotPasswordLink = findViewById(R.id.forgot_password_link)
        signUpLink = findViewById(R.id.sign_up_link)
        biometricButton = findViewById(R.id.biometric_button)
    }

    private fun handleIntentExtras() {
        // Check for pre-filled email from registration or password reset
        val preFilledEmail = intent.getStringExtra("email")
        val message = intent.getStringExtra("message")

        preFilledEmail?.let {
            emailInput.setText(it)
            Log.d("LoginActivity", "Pre-filled email: $it")
        }

        message?.let {
            // Show message after a short delay to ensure activity is fully loaded
            Handler(Looper.getMainLooper()).postDelayed({
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }, 500)
            Log.d("LoginActivity", "Message from previous activity: $it")
        }
    }


    private fun setupObservers() {
        authViewModel.isLoading.observe(this) { isLoading ->
            signInButton.isEnabled = !isLoading
            signInButton.text = if (isLoading) "Signing In..." else "Sign in"
        }

        authViewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                authViewModel.clearErrorMessage()
            }
        }

        authViewModel.successMessage.observe(this) { successMessage ->
            successMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                authViewModel.clearSuccessMessage()
            }
        }

        authViewModel.authState.observe(this) { authState ->
            when (authState) {
                is AuthViewModel.AuthState.SignedIn -> {
                    // Navigate to dashboard activity
                    val intent = Intent(this, DashboardActivity::class.java)
                    startActivity(intent)

                    // Offer to save biometric credentials after successful login
                    offerToSaveBiometricCredentials()

                }
                is AuthViewModel.AuthState.EmailVerificationPending -> {
                    // Navigate back to login with message
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.putExtra("message", "Account created! Please verify your email then sign in.")
                    intent.putExtra("email", authState.email)
                    startActivity(intent)
                    finish()
                }
                else -> { /* Handle other states */ }
            }
        }
    }

    private fun setupClickListeners() {
        signInButton.setOnClickListener {
            authViewModel.signIn(
                email = emailInput.text.toString().trim(),
                password = passwordInput.text.toString()
            )
        }

        forgotPasswordLink.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        signUpLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        biometricButton.setOnClickListener {
            startBiometricAuthentication()
        }
    }


    // Biometric Authentication Setup
    private fun setupBiometricAuth() {
        val executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this as FragmentActivity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED -> {
                            Toast.makeText(this@LoginActivity, "Authentication cancelled", Toast.LENGTH_SHORT).show()
                        }
                        BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                            Toast.makeText(this@LoginActivity, "No biometrics enrolled", Toast.LENGTH_LONG).show()
                        }
                        BiometricPrompt.ERROR_HW_NOT_PRESENT -> {
                            Toast.makeText(this@LoginActivity, "Biometric hardware not available", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(this@LoginActivity, "Authentication error: $errString", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(this@LoginActivity, "Authentication successful!", Toast.LENGTH_SHORT).show()
                    handleBiometricSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@LoginActivity, "Authentication failed. Try again.", Toast.LENGTH_SHORT).show()
                }
            })

        // Create the prompt info
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("SHEGuard Biometric Authentication")
            .setSubtitle("Use your fingerprint or face to sign in securely")
            .setDescription("Place your finger on the sensor or look at the camera")
            .setNegativeButtonText("Use Password")
            .setConfirmationRequired(false)
            .build()
    }

    private fun checkBiometricAvailability() {
        val biometricManager = BiometricManager.from(this)

        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // Biometric authentication is available
                biometricButton.visibility = View.VISIBLE
                updateBiometricButtonText("Quick & Secure Access", "Use fingerprint or face unlock")
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                // Device doesn't have biometric hardware
                biometricButton.visibility = View.GONE
                Toast.makeText(this, "No biometric hardware detected", Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                // Biometric hardware is currently unavailable
                biometricButton.visibility = View.VISIBLE
                updateBiometricButtonText("Biometric Unavailable", "Hardware temporarily unavailable")
                biometricButton.isEnabled = false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // User hasn't enrolled any biometrics
                biometricButton.visibility = View.VISIBLE
                updateBiometricButtonText("Setup Biometrics", "Enroll fingerprint or face in Settings")
                biometricButton.setOnClickListener {
                    promptToEnrollBiometrics()
                }
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                biometricButton.visibility = View.GONE
                Toast.makeText(this, "Security update required for biometrics", Toast.LENGTH_LONG).show()
            }
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                biometricButton.visibility = View.GONE
                Toast.makeText(this, "Biometric authentication not supported", Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                biometricButton.visibility = View.GONE
                Toast.makeText(this, "Biometric status unknown", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startBiometricAuthentication() {
        val biometricManager = BiometricManager.from(this)

        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // Check if user has saved credentials
                if (getSavedEmail() != null && getSavedPassword() != null) {
                    biometricPrompt.authenticate(promptInfo)
                } else {
                    // First time setup
                    showBiometricSetupDialog()
                }
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                promptToEnrollBiometrics()
            }
            else -> {
                Toast.makeText(this, "Biometric authentication not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleBiometricSuccess() {
        // Use saved credentials for automatic sign in
        val savedEmail = getSavedEmail()
        val savedPassword = getSavedPassword()

        if (savedEmail != null && savedPassword != null) {
            // Auto-sign in with saved credentials
            authViewModel.signIn(savedEmail, savedPassword)
        } else {
            // No saved credentials - direct navigation (less secure)
            Toast.makeText(this, "Please set up biometric login first", Toast.LENGTH_LONG).show()
        }
    }


    // Credential Storage (Encrypted)
    private fun saveCredentialsForBiometric(email: String, password: String) {
        try {
            val sharedPreferences = getEncryptedSharedPreferences()
            sharedPreferences.edit()
                .putString("biometric_email", email)
                .putString("biometric_password", password)
                .putBoolean("biometric_enabled", true)
                .apply()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to save biometric credentials", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun getSavedEmail(): String? {
        return try {
            val sharedPreferences = getEncryptedSharedPreferences()
            if (sharedPreferences.getBoolean("biometric_enabled", false)) {
                sharedPreferences.getString("biometric_email", null)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getSavedPassword(): String? {
        return try {
            val sharedPreferences = getEncryptedSharedPreferences()
            if (sharedPreferences.getBoolean("biometric_enabled", false)) {
                sharedPreferences.getString("biometric_password", null)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getEncryptedSharedPreferences(): SharedPreferences {
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            this,
            "biometric_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }


    // Helper Methods
    private fun updateBiometricButtonText(title: String, subtitle: String) {
        try {
            // Find TextViews within the biometric button LinearLayout
            val titleTextView = findTextViewByText(biometricButton, "Quick & Secure Access")
            val subtitleTextView = findTextViewByText(biometricButton, "Use fingerprint or face unlock")

            titleTextView?.text = title
            subtitleTextView?.text = subtitle
        } catch (e: Exception) {
            // If we can't find the specific TextViews, that's okay
            e.printStackTrace()
        }
    }

    private fun findTextViewByText(parent: LinearLayout, searchText: String): TextView? {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is TextView && child.text.toString().contains(searchText.split(" ")[0])) {
                return child
            }
            if (child is LinearLayout) {
                val found = findTextViewByText(child, searchText)
                if (found != null) return found
            }
        }
        return null
    }

    private fun promptToEnrollBiometrics() {
        AlertDialog.Builder(this)
            .setTitle("Setup Biometric Authentication")
            .setMessage("To use biometric login, please enroll your fingerprint or face in device Settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                        startActivity(intent)
                    } catch (ex: Exception) {
                        Toast.makeText(this, "Please manually go to Settings > Security > Biometrics", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBiometricSetupDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Biometric Login")
            .setMessage("To use biometric authentication, please sign in with your email and password first. We'll then save your credentials securely for future biometric logins.")
            .setPositiveButton("OK") { _, _ ->
                Toast.makeText(this, "Please sign in with email and password to enable biometric login", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun offerToSaveBiometricCredentials() {
        val biometricManager = BiometricManager.from(this)

        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS &&
            getSavedEmail() == null &&
            emailInput.text.toString().isNotEmpty() &&
            passwordInput.text.toString().isNotEmpty()) {

            AlertDialog.Builder(this)
                .setTitle("Enable Biometric Login")
                .setMessage("Would you like to enable biometric authentication for faster login next time?")
                .setPositiveButton("Yes") { _, _ ->
                    saveCredentialsForBiometric(
                        emailInput.text.toString().trim(),
                        passwordInput.text.toString()
                    )
                    Toast.makeText(this, "Biometric login enabled!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Not Now", null)
                .show()
        }
    }

    fun clearBiometricCredentials() {
        try {
            val sharedPreferences = getEncryptedSharedPreferences()
            sharedPreferences.edit()
                .remove("biometric_email")
                .remove("biometric_password")
                .putBoolean("biometric_enabled", false)
                .apply()
            Toast.makeText(this, "Biometric credentials cleared", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}