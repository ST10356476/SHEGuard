package com.iiest10356476.sheguard.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.data.repository.UpdateUserDataRepository
import kotlinx.coroutines.launch

class ProfileActivity :  BaseActivity() {


    // UI Components
    private lateinit var firstNameEdit: EditText
    private lateinit var lastNameEdit: EditText
    private lateinit var emailEdit: EditText
    private lateinit var phoneEdit: EditText
    private lateinit var dateOfBirthEdit: EditText
    private lateinit var saveButton: LinearLayout
    private lateinit var cancelButton: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
        loadUserProfile()
    }

    private fun setupUI() {
        // Set up back button
        val backButton = findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            finish() // Return to Settings
        }

        // Initialize form fields
        firstNameEdit = findViewById(R.id.first_name_edit)
        lastNameEdit = findViewById(R.id.last_name_edit)
        emailEdit = findViewById(R.id.email_edit)
        dateOfBirthEdit = findViewById(R.id.date_of_birth_edit)

        // Initialize buttons
        saveButton = findViewById(R.id.save_button)
        cancelButton = findViewById(R.id.cancel_button)

        // Set up button click listeners
        saveButton.setOnClickListener {
            saveProfile()
        }

        cancelButton.setOnClickListener {
            finish() // Return to Settings without saving
        }
    }
    private val userRepo = UpdateUserDataRepository()

    private fun loadUserProfile() {
        lifecycleScope.launch {
            val result = userRepo.getUserProfile()
            result.onSuccess { user ->
                firstNameEdit.setText(user.fullName.split(" ").firstOrNull() ?: "")
                lastNameEdit.setText(user.fullName.split(" ").drop(1).joinToString(" "))
                emailEdit.setText(user.email)
                dateOfBirthEdit.setText(user.dateOfBirth)
            }.onFailure {
                Toast.makeText(this@ProfileActivity, it.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    //Took away the Phone Number, and every field is optional
    private fun saveProfile() {
        val firstName = firstNameEdit.text.toString().trim()
        val lastName = lastNameEdit.text.toString().trim()
        val dateOfBirth = dateOfBirthEdit.text.toString().trim()

        val fullName = listOf(firstName, lastName).filter { it.isNotEmpty() }.joinToString(" ")


        val updates = mutableMapOf<String, Any>()
        if (fullName.isNotEmpty()) updates["fullName"] = fullName
        if (dateOfBirth.isNotEmpty()) updates["dateOfBirth"] = dateOfBirth
        updates["lastLoginAt"] = System.currentTimeMillis()

        if (updates.isEmpty()) {
            Toast.makeText(this, "Nothing to update", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val result = userRepo.updateUserProfile(updates)
            result.onSuccess {
                Toast.makeText(this@ProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure {
                Toast.makeText(this@ProfileActivity, it.message, Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}