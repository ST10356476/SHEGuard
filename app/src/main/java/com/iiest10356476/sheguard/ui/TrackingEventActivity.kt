package com.iiest10356476.sheguard.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.services.LiveLocationTracker

class TrackingEventActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tracking_event)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set up back button
        val backButton = findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }

        // Set up start event tracking button
        val startEventButton = findViewById<LinearLayout>(R.id.start_event_button)
        startEventButton.setOnClickListener {
            val intent = Intent(this, TrackingEventOperationActivity::class.java)
            startActivity(intent)
        }

        // Set up live tracking button
        val liveTrackingButton = findViewById<LinearLayout>(R.id.livetrack_button)
        liveTrackingButton.setOnClickListener {
            val intent = Intent(this, LiveTrackingMapActivity::class.java)
            startActivity(intent)
        }

        // Set up bottom navigation
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    true
                }
                R.id.nav_shetrack -> {
                    // Already on SHETrack - no action needed
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
                    // Navigate to Settings activity
                    // val intent = Intent(this, SettingsActivity::class.java)
                    // startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Set the current item as selected (SHETrack)
        bottomNavigation.selectedItemId = R.id.nav_shetrack
    }
}