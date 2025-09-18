package com.iiest10356476.sheguard.ui

import com.iiest10356476.sheguard.ui.auth.NewPasswordActivity
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import android.app.Dialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.iiest10356476.sheguard.R
import android.widget.RadioButton
import android.widget.RadioGroup
import com.iiest10356476.sheguard.utils.LanguageHelper

class SettingsActivity : BaseActivity() {

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
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Change Password button
        passwordButton.setOnClickListener {
            val intent = Intent(this, com.iiest10356476.sheguard.ui.auth.NewPasswordActivity::class.java)
            intent.putExtra(com.iiest10356476.sheguard.ui.auth.NewPasswordActivity.EXTRA_FROM_SETTINGS, true)
            startActivity(intent)
        }

        // Change Language button
        languageButton.setOnClickListener {
            showLanguageSelectionDialog()
        }

        // Policy button
        policyButton.setOnClickListener {
            val intent = Intent(this, PolicyActivity::class.java)
            startActivity(intent)
        }

        // Delete Account button - Show confirmation dialog
        deleteAccountButton.setOnClickListener {
            showDeleteAccountConfirmation()
        }
    }

    private fun showLanguageSelectionDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_language_selection)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(true)

        // Get current language
        val currentLanguage = LanguageHelper.getSavedLanguage(this)

        // Find views in dialog
        val radioGroup = dialog.findViewById<RadioGroup>(R.id.language_radio_group)
        val radioEnglish = dialog.findViewById<RadioButton>(R.id.radio_english)
        val radioAfrikaans = dialog.findViewById<RadioButton>(R.id.radio_afrikaans)
        val radioZulu = dialog.findViewById<RadioButton>(R.id.radio_zulu)
        val radioXhosa = dialog.findViewById<RadioButton>(R.id.radio_xhosa)
        val cancelButton = dialog.findViewById<LinearLayout>(R.id.cancel_button)
        val applyButton = dialog.findViewById<LinearLayout>(R.id.apply_button)

        // Set current language as selected
        when (currentLanguage) {
            LanguageHelper.LANGUAGE_ENGLISH -> radioEnglish.isChecked = true
            LanguageHelper.LANGUAGE_AFRIKAANS -> radioAfrikaans.isChecked = true
            LanguageHelper.LANGUAGE_ZULU -> radioZulu.isChecked = true
            LanguageHelper.LANGUAGE_XHOSA -> radioXhosa.isChecked = true
        }

        // Cancel button click
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        // Apply button click
        applyButton.setOnClickListener {
            val selectedRadioId = radioGroup.checkedRadioButtonId
            val selectedLanguage = when (selectedRadioId) {
                R.id.radio_english -> LanguageHelper.LANGUAGE_ENGLISH
                R.id.radio_afrikaans -> LanguageHelper.LANGUAGE_AFRIKAANS
                R.id.radio_zulu -> LanguageHelper.LANGUAGE_ZULU
                R.id.radio_xhosa -> LanguageHelper.LANGUAGE_XHOSA
                else -> LanguageHelper.LANGUAGE_ENGLISH
            }

            // Save the selected language
            LanguageHelper.saveLanguage(this, selectedLanguage)

            // Show confirmation message
            Toast.makeText(
                this,
                "Language changed to ${LanguageHelper.getLanguageDisplayName(selectedLanguage)}",
                Toast.LENGTH_SHORT
            ).show()

            dialog.dismiss()

            // Restart activity to apply language change
            onLanguageChanged()
        }

        dialog.show()
    }

    private fun showDeleteAccountConfirmation() {
        // Create custom dialog
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_delete_account)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(true)

        // Find buttons in custom layout
        val cancelButton = dialog.findViewById<LinearLayout>(R.id.cancel_button)
        val deleteButton = dialog.findViewById<LinearLayout>(R.id.delete_button)

        // Cancel button click
        cancelButton.setOnClickListener {
            dialog.dismiss()
            // Stay on settings page - no additional action needed
        }

        // Delete button click
        deleteButton.setOnClickListener {
            dialog.dismiss()
            // TODO: Implement actual account deletion logic
            performAccountDeletion()
        }

        dialog.show()
    }

    private fun performAccountDeletion() {
        // TODO: Add your actual account deletion logic here
        // This might include:
        // 1. API call to delete user data from server
        // 2. Clear local database/preferences
        // 3. Sign out user
        // 4. Navigate to login/welcome screen

        // For now, show a placeholder message
        Toast.makeText(
            this,
            "Account deletion feature will be implemented",
            Toast.LENGTH_LONG
        ).show()

        // Example of what you might do after successful deletion:
        // clearUserData()
        // val intent = Intent(this, WelcomeActivity::class.java)
        // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        // startActivity(intent)
        // finish()
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.itemIconTintList = null
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, DashboardActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_shetrack -> {
                    val intent = Intent(this, TrackingEventActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_vault -> {
                    // Navigate to vault with authentication first
                    val intent = Intent(this, SecureActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_community -> {
                    val intent = Intent(this, SheCareActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_settings -> {
                    // Already on settings - no action needed
                    true
                }
                else -> false
            }
        }

        // Set the current item as selected (Settings)
        bottomNavigation.selectedItemId = R.id.nav_settings
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