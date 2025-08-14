package com.iiest10356476.sheguard.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.ui.auth.LoginActivity
import com.iiest10356476.sheguard.viewmodel.AuthViewModel

class MainActivity : AppCompatActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "MainActivity created - determining user flow")

        // Initialize SharedPreferences
        prefs = getSharedPreferences("sheguard_prefs", MODE_PRIVATE)

        // Check user flow
        determineUserFlow()
    }

    private fun determineUserFlow() {
        // Check if this is the first time the app is opened
        val isFirstTime = prefs.getBoolean("is_first_time", true)

        Log.d("MainActivity", "Is first time: $isFirstTime")

        if (isFirstTime) {
            // First time user - show GetStarted
            Log.d("MainActivity", "🆕 First time user - going to GetStarted")
            startActivity(Intent(this, GetStartedActivity::class.java))
            finish()
        } else {
            // Returning user - check authentication state
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
