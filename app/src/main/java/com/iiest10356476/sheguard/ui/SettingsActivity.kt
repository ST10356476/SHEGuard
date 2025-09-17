package com.iiest10356476.sheguard.ui

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import android.app.Dialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.data.repository.SettingsRepo
import com.iiest10356476.sheguard.ui.auth.LoginActivity

class SettingsActivity : AppCompatActivity() {

    // UI Components
    private lateinit var backButton: ImageButton
    private lateinit var profilesButton: LinearLayout
    private lateinit var passwordButton: LinearLayout
    private lateinit var languageButton: LinearLayout
    private lateinit var policyButton: LinearLayout
    private lateinit var deleteAccountButton: LinearLayout
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Handle back press with modern approach
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        // Initialize views
        initializeViews()

        // Setup click listeners
        setupClickListeners()

        // Setup bottom navigation
        setupBottomNavigation()
    }

    private fun initializeViews() {
        // Initialize all UI components with correct IDs from your layout
        backButton = findViewById(R.id.back_button)
        profilesButton = findViewById(R.id.profiles_button)
        passwordButton = findViewById(R.id.password_button)
        languageButton = findViewById(R.id.language_button)
        policyButton = findViewById(R.id.policy_button)
        deleteAccountButton = findViewById(R.id.delete_account_button)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
    }

    private fun setupClickListeners() {

        // Back button - return to previous screen
        backButton.setOnClickListener {
            finish()
        }

        // Profile button
        profilesButton.setOnClickListener {
            // TODO: Navigate to Profile Activity
            // val intent = Intent(this, ProfileActivity::class.java)
            // startActivity(intent)

            // For now, show a placeholder message
            showPlaceholderMessage("Profile")
        }

        // Change Password button
        passwordButton.setOnClickListener {
            // TODO: Navigate to Change Password Activity
            // val intent = Intent(this, ChangePasswordActivity::class.java)
            // startActivity(intent)

            // For now, show a placeholder message
            showPlaceholderMessage("Change Password")
        }

        // Change Language button
        languageButton.setOnClickListener {
            // TODO: Navigate to Change Language Activity
            // val intent = Intent(this, ChangeLanguageActivity::class.java)
            // startActivity(intent)

            // For now, show a placeholder message
            showPlaceholderMessage("Change Language")
        }

        // Policy button
        policyButton.setOnClickListener {
            // TODO: Navigate to Policy Activity
            // val intent = Intent(this, PolicyActivity::class.java)
            // startActivity(intent)

            // For now, show a placeholder message
            showPlaceholderMessage("Policy")
        }

        // Delete Account button - Show confirmation dialog
        deleteAccountButton.setOnClickListener {
            showDeleteAccountConfirmation()
        }
    }

    private fun showDeleteAccountConfirmation() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_delete_account)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(true)

        val cancelButton = dialog.findViewById<LinearLayout>(R.id.cancel_button)
        val deleteButton = dialog.findViewById<LinearLayout>(R.id.delete_button)

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        deleteButton.setOnClickListener {
            dialog.dismiss()
            // TODO: Implement actual account deletion logic
            performAccountDeletion()
        }

        dialog.show()
    }

    private fun performAccountDeletion() {
        val repo = SettingsRepo()

        repo.deleteCurrentUserData { success, error ->
            if (success) {
                Toast.makeText(this, "Account deleted", Toast.LENGTH_LONG).show()

                // Go to LoginActivity and clear back stack
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
            }
        }
    }



    private fun setupBottomNavigation() {
        // Set the current selected item based on your bottom navigation menu
        // Update this ID to match your actual menu item for settings
        bottomNavigationView.selectedItemId = R.id.nav_settings

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Navigate to Dashboard/Main Activity
                    val intent = Intent(this, DashboardActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    // Already on settings page
                    true
                }
                R.id.nav_vault -> {
                    // Navigate to Secure Vault
                    val intent = Intent(this, SecureVault::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_shetrack -> {
                    // Navigate to SHETrack
                    val intent = Intent(this, TrackingEventActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                // Add other navigation items as needed
                else -> false
            }
        }
    }

    private fun showPlaceholderMessage(feature: String) {
        // Temporary method to show which button was clicked
        // You can replace this with actual navigation when you create the other activities
        Toast.makeText(
            this,
            "$feature page will be implemented",
            Toast.LENGTH_SHORT
        ).show()
    }
}