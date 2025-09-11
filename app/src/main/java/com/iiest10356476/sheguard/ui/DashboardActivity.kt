package com.iiest10356476.sheguard.ui

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.iiest10356476.sheguard.R

class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        // Adjust padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set up secure vault button click
        val openVaultButton = findViewById<LinearLayout>(R.id.secure_vault_button)
        openVaultButton.setOnClickListener {
            val intent = Intent(this, SecureVault::class.java)
            startActivity(intent)
        }

        // Set up SHETrack button click
        val sheTrackButton = findViewById<LinearLayout>(R.id.she_track_button)
        sheTrackButton.setOnClickListener {
            val intent = Intent(this, TrackingEventActivity::class.java)
            startActivity(intent)
        }

        // Set up panic button click
        val panicButton = findViewById<LinearLayout>(R.id.panic_button)
        panicButton.setOnClickListener {
            // Handle panic button functionality
            // This should trigger emergency protocols
        }

        // Set up bottom navigation
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on home/dashboard - no action needed
                    true
                }
                R.id.nav_shetrack -> {
                    val intent = Intent(this, TrackingEventActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_vault -> {
                    val intent = Intent(this, SecureVault::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_community -> {
                    // Navigate to Community activity
                    // val intent = Intent(this, CommunityActivity::class.java)
                    // startActivity(intent)
                    true
                }
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Set the current item as selected (Home)
        bottomNavigation.selectedItemId = R.id.nav_home
    }
}