package com.iiest10356476.sheguard.ui.auth

import android.app.DatePickerDialog
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.ui.DashboardActivity
import com.iiest10356476.sheguard.viewmodel.AuthViewModel
import java.util.Locale

class RegisterActivity : AppCompatActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var fullNameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var dobInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var termsCheckbox: CheckBox
    private lateinit var signUpButton: Button
    private lateinit var signInLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        initViews()
        setupObservers()
        setupClickListeners()
    }

    private fun initViews() {
        fullNameInput = findViewById(R.id.full_name_input)
        emailInput = findViewById(R.id.email_input)
        dobInput = findViewById(R.id.dob_input)
        passwordInput = findViewById(R.id.password_input)
        confirmPasswordInput = findViewById(R.id.confirm_password_input)
        termsCheckbox = findViewById(R.id.terms_checkbox)
        signUpButton = findViewById(R.id.sign_up_button)
        signInLink = findViewById(R.id.sign_in_link)
    }

    private fun setupObservers() {
        authViewModel.isLoading.observe(this) { isLoading ->
            signUpButton.isEnabled = !isLoading
            signUpButton.text = if (isLoading) "Creating Account..." else "Sign up"
        }

        authViewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                authViewModel.clearErrorMessage()
            }
        }

        authViewModel.successMessage.observe(this) { successMessage ->
            successMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                authViewModel.clearSuccessMessage()
            }
        }

        authViewModel.authState.observe(this) { authState ->
            Log.d("RegisterActivity", "Auth state changed: $authState")

            when (authState) {
                is AuthViewModel.AuthState.EmailVerificationPending -> {
                    // Navigate back to Login with message
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.putExtra("message", "Account created! Please verify your email then sign in.")
                    intent.putExtra("email", authState.email)
                    startActivity(intent)
                    finish()
                }
                else -> {
                    Log.d("RegisterActivity", "Other auth state: $authState")
                }
            }
        }
    }

    private fun setupClickListeners() {
        signUpButton.setOnClickListener {
            if (!termsCheckbox.isChecked) {
                Toast.makeText(this, "Please accept the terms and conditions", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (passwordInput.text.toString() != confirmPasswordInput.text.toString()) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authViewModel.signUp(
                email = emailInput.text.toString().trim(),
                password = passwordInput.text.toString(),
                fullName = fullNameInput.text.toString().trim(),
                dateOfBirth = dobInput.text.toString().trim()
            )
        }

        signInLink.setOnClickListener {
            // Navigate to sign in activity
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Date picker for DOB
        dobInput.setOnClickListener {
            // Implement date picker dialog
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val maxDate = calendar.timeInMillis

        // Default to 18 years ago
        calendar.add(Calendar.YEAR, -18)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)

                if (isValidAge(selectedCalendar)) {
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val formattedDate = dateFormat.format(selectedCalendar.time)
                    dobInput.setText(formattedDate)
                } else {
                    Toast.makeText(
                        this,
                        "You must be at least 13 years old to use SHEGuard",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.maxDate = maxDate
        datePickerDialog.show()
    }

    // Helper function to validate minimum age
    private fun isValidAge(selectedDate: Calendar): Boolean {
        val today = Calendar.getInstance()
        val minimumAge = 13 // Minimum age for the app

        // Calculate age
        var age = today.get(Calendar.YEAR) - selectedDate.get(Calendar.YEAR)

        // Check if birthday hasn't occurred this year
        if (today.get(Calendar.DAY_OF_YEAR) < selectedDate.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        return age >= minimumAge
    }
}