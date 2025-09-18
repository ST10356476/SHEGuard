package com.iiest10356476.sheguard.ui

import android.accessibilityservice.AccessibilityService
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.services.VolumeButtonService
import com.iiest10356476.sheguard.ui.auth.LoginActivity
import com.iiest10356476.sheguard.viewmodel.AuthViewModel

class MainActivity :  BaseActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("MainActivity", "MainActivity created - determining user flow")

        // Accessibility service check
        if (!isAccessibilityServiceEnabled(VolumeButtonService::class.java)) {
            Log.d("MainActivity", "Accessibility Service not enabled - showing dialog")
            showAccessibilityDialog()
        }

        // Initialize SharedPreferences
        prefs = getSharedPreferences("sheguard_prefs", MODE_PRIVATE)

        // Check user flow
        determineUserFlow()
    }
    private val REQUEST_RECORD_AUDIO = 101

    private fun checkAudioPermission() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
        }
    }

    private fun showAccessibilityDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Safety Shortcut")
            .setMessage(
                "To use SHEGuard’s emergency feature, please enable the Accessibility Service.\n\n" +
                        "This lets you press the volume down button 8 times to start a panic recording " +
                        "for your safety."
            )
            .setCancelable(false)
            .setPositiveButton("Go to Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun isAccessibilityServiceEnabled(service: Class<out AccessibilityService>): Boolean {
        val expectedComponentName = ComponentName(this, service)
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (ComponentName.unflattenFromString(componentName) == expectedComponentName) {
                return true
            }
        }
        return false
    }

    private fun determineUserFlow() {
        val isFirstTime = prefs.getBoolean("is_first_time", true)
        Log.d("MainActivity", "Is first time: $isFirstTime")

        if (isFirstTime) {
            Log.d("MainActivity", "🆕 First time user - going to GetStarted")
            startActivity(Intent(this, GetStartedActivity::class.java))
            finish()
        } else {
            Log.d("MainActivity", "🔄 Returning user - checking auth state")
            checkAuthenticationForReturningUser()
        }
    }

    private fun checkAuthenticationForReturningUser() {
        authViewModel.authState.observe(this) { authState ->
            Log.d("MainActivity", "Auth state for returning user: $authState")

            when (authState) {
                is AuthViewModel.AuthState.SignedIn -> {
                    Log.d("MainActivity", "✅ Returning user signed in - going to Dashboard")
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                }
                is AuthViewModel.AuthState.SignedOut -> {
                    Log.d("MainActivity", "❌ Returning user not signed in - going to Login")
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                is AuthViewModel.AuthState.Loading -> {
                    Log.d("MainActivity", "⏳ Loading auth state...")
                    // Stay here while loading
                }
                else -> {
                    Log.d("MainActivity", "Other auth state - going to Login")
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }
}
