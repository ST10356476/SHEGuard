package com.iiest10356476.sheguard.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.data.repository.UpdatePasswordRepository
import com.iiest10356476.sheguard.ui.SettingsActivity
import kotlinx.coroutines.launch

class NewPasswordActivity : AppCompatActivity() {

    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var termsCheckbox: CheckBox
    private lateinit var doneButton: Button
    private var backButton: ImageButton? = null

    private val updatePasswordRepo = UpdatePasswordRepository()

    companion object {
        const val EXTRA_FROM_SETTINGS = "from_settings"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_password)

        initViews()
        setupBackNavigation()
        setupClickListeners()
    }

    private fun initViews() {
        passwordInput = findViewById(R.id.password_input)
        confirmPasswordInput = findViewById(R.id.confirm_password_input)
        termsCheckbox = findViewById(R.id.terms_checkbox)
        doneButton = findViewById(R.id.done_button)

        backButton = try {
            findViewById(R.id.back_button)
        } catch (e: Exception) {
            null
        }
    }

    private fun setupBackNavigation() {
        // Handle hardware back
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBack()
            }
        })

        // Handle UI back
        backButton?.setOnClickListener {
            navigateBack()
        }
    }

    private fun navigateBack() {
        val isFromSettings = intent.getBooleanExtra(EXTRA_FROM_SETTINGS, false)
        if (isFromSettings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        } else {
            finish()
        }
    }

    private fun setupClickListeners() {
        doneButton.setOnClickListener {
            val currentPassword = passwordInput.text.toString().trim()
            val newPassword = confirmPasswordInput.text.toString().trim()

            if (currentPassword.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in both fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!termsCheckbox.isChecked) {
                Toast.makeText(this, "Please accept the terms and conditions", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val result = updatePasswordRepo.updatePassword(currentPassword, newPassword)
                result.onSuccess {
                    Toast.makeText(
                        this@NewPasswordActivity,
                        "Password updated successfully! Please log in again.",
                        Toast.LENGTH_LONG
                    ).show()

                    // ✅ Force logout
                    updatePasswordRepo.auth.signOut()

                    val intent = Intent(this@NewPasswordActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }.onFailure { e ->
                    Toast.makeText(
                        this@NewPasswordActivity,
                        e.message ?: "Error updating password",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
