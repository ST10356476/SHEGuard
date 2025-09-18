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
import com.iiest10356476.sheguard.R

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
        phoneEdit = findViewById(R.id.phone_edit)
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

    private fun loadUserProfile() {
        // TODO: Load user data from database/shared preferences
        // For now, using placeholder data

        // Example of how you might load saved data:
        // val sharedPref = getSharedPreferences("user_profile", Context.MODE_PRIVATE)
        // firstNameEdit.setText(sharedPref.getString("first_name", ""))
        // lastNameEdit.setText(sharedPref.getString("last_name", ""))
        // emailEdit.setText(sharedPref.getString("email", ""))
        // phoneEdit.setText(sharedPref.getString("phone", ""))
        // dateOfBirthEdit.setText(sharedPref.getString("date_of_birth", ""))

        // Placeholder data for demonstration
        firstNameEdit.setText("")
        lastNameEdit.setText("")
        emailEdit.setText("")
        phoneEdit.setText("")
        dateOfBirthEdit.setText("")
    }

    private fun saveProfile() {
        // Get values from form fields
        val firstName = firstNameEdit.text.toString().trim()
        val lastName = lastNameEdit.text.toString().trim()
        val email = emailEdit.text.toString().trim()
        val phone = phoneEdit.text.toString().trim()
        val dateOfBirth = dateOfBirthEdit.text.toString().trim()

        // Validate required fields
        if (firstName.isEmpty()) {
            firstNameEdit.error = "First name is required"
            firstNameEdit.requestFocus()
            return
        }

        if (lastName.isEmpty()) {
            lastNameEdit.error = "Last name is required"
            lastNameEdit.requestFocus()
            return
        }

        if (email.isEmpty()) {
            emailEdit.error = "Email is required"
            emailEdit.requestFocus()
            return
        }

        if (!isValidEmail(email)) {
            emailEdit.error = "Please enter a valid email"
            emailEdit.requestFocus()
            return
        }

        // TODO: Save user data to database/shared preferences
        // Example of how you might save data:
        // val sharedPref = getSharedPreferences("user_profile", Context.MODE_PRIVATE)
        // with(sharedPref.edit()) {
        //     putString("first_name", firstName)
        //     putString("last_name", lastName)
        //     putString("email", email)
        //     putString("phone", phone)
        //     putString("date_of_birth", dateOfBirth)
        //     apply()
        // }

        // TODO: Also update user data on your server/backend if applicable

        // Show success message
        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()

        // Return to Settings
        finish()
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}