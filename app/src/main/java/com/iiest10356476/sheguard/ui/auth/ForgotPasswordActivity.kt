package com.iiest10356476.sheguard.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.viewmodel.AuthViewModel

class ForgotPasswordActivity : AppCompatActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var emailInput: EditText
    private lateinit var sendResetButton: Button
    private lateinit var signInLink: TextView
    private lateinit var backButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        initViews()
        setupObservers()
        setupClickListeners()

        // Pre-fill email if coming from login screen
        handleIntentExtras()
    }

    private fun initViews() {
        emailInput = findViewById(R.id.email_input)
        sendResetButton = findViewById(R.id.send_otp_button) // Keep same ID from XML
        signInLink = findViewById(R.id.sign_in_link)
        backButton = findViewById(R.id.back_button)
    }

    private fun handleIntentExtras() {
        // Check if email was passed from login screen
        val preFilledEmail = intent.getStringExtra("email")
        preFilledEmail?.let {
            emailInput.setText(it)
        }
    }

    private fun setupObservers() {
        authViewModel.isLoading.observe(this) { isLoading ->
            sendResetButton.isEnabled = !isLoading
            sendResetButton.text = if (isLoading) "Sending Reset Email..." else "Send Reset Email"

            // Disable email input while loading
            emailInput.isEnabled = !isLoading
        }

        authViewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                showErrorDialog(it)
                authViewModel.clearErrorMessage()
            }
        }

        authViewModel.successMessage.observe(this) { successMessage ->
            successMessage?.let {
                showSuccessDialog(it)
                authViewModel.clearSuccessMessage()
            }
        }
    }

    private fun setupClickListeners() {
        sendResetButton.setOnClickListener {
            val email = emailInput.text.toString().trim()

            if (validateEmail(email)) {
                authViewModel.resetPassword(email)
            }
        }

        signInLink.setOnClickListener {
            navigateToSignIn()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                emailInput.error = "Please enter your email address"
                emailInput.requestFocus()
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailInput.error = "Please enter a valid email address"
                emailInput.requestFocus()
                false
            }
            else -> {
                emailInput.error = null
                true
            }
        }
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Reset Password Failed")
            .setMessage(message)
            .setPositiveButton("Try Again") { dialog, _ ->
                dialog.dismiss()
                emailInput.requestFocus()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showSuccessDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Reset Email Sent!")
            .setMessage("$message\n\nPlease check your email inbox and follow the instructions to reset your password.\n\nAfter resetting your password, return to the app to sign in.")
            .setCancelable(false)
            .setPositiveButton("Check Email") { dialog, _ ->
                dialog.dismiss()
                // Optional: Open email app
                openEmailApp()
            }
            .setNegativeButton("Back to Sign In") { dialog, _ ->
                dialog.dismiss()
                navigateToSignIn()
            }
            .setNeutralButton("Resend") { dialog, _ ->
                dialog.dismiss()
                // Allow user to send another reset email
                Toast.makeText(this, "You can send another reset email if needed", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun openEmailApp() {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_APP_EMAIL)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            // If no email app is found, show alternative
            Toast.makeText(this, "Please check your email app manually", Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToSignIn() {
        val intent = Intent(this, LoginActivity::class.java)
        // Pass the email back to login screen for convenience
        intent.putExtra("email", emailInput.text.toString().trim())
        intent.putExtra("message", "Check your email for password reset instructions")
        startActivity(intent)
        finish()
    }

    // Optional: Add this method to help users who might not receive the email
    private fun showEmailTroubleshooting() {
        AlertDialog.Builder(this)
            .setTitle("Didn't receive the email?")
            .setMessage("""
                If you don't see the reset email:
                
                • Check your spam/junk folder
                • Make sure you entered the correct email
                • Wait a few minutes for delivery
                • Check if the email address is associated with your SHEGuard account
                
                You can try sending another reset email or contact support if the problem persists.
            """.trimIndent())
            .setPositiveButton("Send Another Email") { dialog, _ ->
                dialog.dismiss()
                val email = emailInput.text.toString().trim()
                if (validateEmail(email)) {
                    authViewModel.resetPassword(email)
                }
            }
            .setNegativeButton("Contact Support") { dialog, _ ->
                dialog.dismiss()
                //support contact functionality
                Toast.makeText(this, "Please contact support at support@sheguard.com", Toast.LENGTH_LONG).show()
            }
            .setNeutralButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Override back button to provide helpful navigation
    override fun onBackPressed() {
        super.onBackPressed()
        AlertDialog.Builder(this)
            .setTitle("Go Back?")
            .setMessage("Do you want to return to the sign in screen?")
            .setPositiveButton("Yes") { _, _ ->
                navigateToSignIn()
            }
            .setNegativeButton("Stay Here") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}