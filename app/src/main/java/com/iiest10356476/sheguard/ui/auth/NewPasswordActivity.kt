package com.iiest10356476.sheguard.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.viewmodel.AuthViewModel

class NewPasswordActivity : AppCompatActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var termsCheckbox: CheckBox
    private lateinit var doneButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_password)

        initViews()
        setupObservers()
        setupClickListeners()
    }

    private fun initViews() {
        passwordInput = findViewById(R.id.password_input)
        confirmPasswordInput = findViewById(R.id.confirm_password_input)
        termsCheckbox = findViewById(R.id.terms_checkbox)
        doneButton = findViewById(R.id.done_button)
    }

    private fun setupObservers() {
        authViewModel.isLoading.observe(this) { isLoading ->
            doneButton.isEnabled = !isLoading
            doneButton.text = if (isLoading) "Updating..." else "Done"
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
                val intent = Intent(this, LoginActivity::class.java)
                intent.putExtra("message", it)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun setupClickListeners() {
        doneButton.setOnClickListener {
            if (!termsCheckbox.isChecked) {
                Toast.makeText(this, "Please accept the terms and conditions", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

        }
    }
}