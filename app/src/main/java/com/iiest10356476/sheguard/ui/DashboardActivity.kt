package com.iiest10356476.sheguard.ui

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.services.PanicService
import kotlinx.coroutines.launch

class DashboardActivity :  BaseActivity() {

    companion object {
        private const val TAG = "DashboardActivity"
    }

    private lateinit var panicService: PanicService
    private var panicCountdownTimer: CountDownTimer? = null

    // Permission launcher for panic button
    private val requestPanicPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Log.d(TAG, "All panic permissions granted")
            showPanicConfirmation()
        } else {
            Log.w(TAG, "Some panic permissions denied")
            Toast.makeText(this, "Permissions required for emergency features", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        // Initialize panic service
        panicService = PanicService(this)

        // Adjust padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
        setupBottomNavigation()
    }

    private fun setupClickListeners() {
        // Set up secure vault button click
        val openVaultButton = findViewById<LinearLayout>(R.id.secure_vault_button)
        openVaultButton.setOnClickListener {
            val intent = Intent(this, SecureActivity::class.java)
            startActivity(intent)
        }

        // Set up SHETrack button click
        val sheTrackButton = findViewById<LinearLayout>(R.id.she_track_button)
        sheTrackButton.setOnClickListener {
            val intent = Intent(this, TrackingEventActivity::class.java)
            startActivity(intent)
        }

        // Set up panic button click - THE MAIN IMPLEMENTATION
        val panicButton = findViewById<LinearLayout>(R.id.panic_button)
        panicButton.setOnClickListener {
            handlePanicButtonPressed()
        }

        // Optional: Long press for immediate panic (no countdown)
        panicButton.setOnLongClickListener {
            handleImmediatePanic()
            true
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.itemIconTintList = null
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

    private fun handlePanicButtonPressed() {
        Log.d(TAG, "Panic button pressed")

        // Check if we have necessary permissions
        if (!hasRequiredPermissions()) {
            requestPanicPermissions.launch(getRequiredPermissions().toTypedArray())
            return
        }

        // Show confirmation with countdown
        showPanicConfirmation()
    }

    private fun handleImmediatePanic() {
        Log.d(TAG, "Immediate panic triggered (long press)")

        if (!hasRequiredPermissions()) {
            requestPanicPermissions.launch(getRequiredPermissions().toTypedArray())
            return
        }

        triggerPanicMode(showConfirmation = false)
    }

    private fun showPanicConfirmation() {
        val builder = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
        builder.setTitle("🚨 Emergency Alert")
        builder.setMessage("Emergency services will be contacted in 10 seconds.\n\nPress CANCEL to abort.")
        builder.setCancelable(true)

        var countdown = 10
        val dialog = builder.create()

        // Update the message with countdown
        panicCountdownTimer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countdown = (millisUntilFinished / 1000).toInt() + 1
                dialog.setMessage("Emergency services will be contacted in $countdown seconds.\n\nPress CANCEL to abort.\n\n💡 Tip: Press volume down 8 times for silent emergency mode")
            }

            override fun onFinish() {
                if (dialog.isShowing) {
                    dialog.dismiss()
                    triggerPanicMode(showConfirmation = true)
                }
            }
        }

        builder.setNegativeButton("CANCEL") { dialogInterface, _ ->
            Log.d(TAG, "Panic cancelled by user")
            panicCountdownTimer?.cancel()
            dialogInterface.dismiss()
            Toast.makeText(this, "Emergency alert cancelled", Toast.LENGTH_SHORT).show()
        }

        builder.setPositiveButton("SEND NOW") { dialogInterface, _ ->
            Log.d(TAG, "Immediate panic confirmed by user")
            panicCountdownTimer?.cancel()
            dialogInterface.dismiss()
            triggerPanicMode(showConfirmation = true)
        }

        dialog.show()
        panicCountdownTimer?.start()

        // Auto-dismiss if activity is destroyed
        dialog.setOnDismissListener {
            panicCountdownTimer?.cancel()
        }
    }

    private fun triggerPanicMode(showConfirmation: Boolean = true) {
        Log.d(TAG, "Triggering panic mode")

        if (showConfirmation) {
            Toast.makeText(this, "🚨 EMERGENCY ALERT SENT 🚨", Toast.LENGTH_LONG).show()
        }

        lifecycleScope.launch {
            try {
                // Include audio recording by default for button panic
                panicService.triggerPanicMode(includeAudioRecording = true)

                // Show success message
                if (showConfirmation) {
                    runOnUiThread {
                        AlertDialog.Builder(this@DashboardActivity)
                            .setTitle("Emergency Alert Sent")
                            .setMessage("Your emergency contacts have been notified with your location.\nAudio recording has started for evidence.")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error triggering panic mode", e)

                runOnUiThread {
                    Toast.makeText(
                        this@DashboardActivity,
                        "Error sending alert: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getRequiredPermissions(): List<String> {
        return listOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO // Added for audio recording
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        panicCountdownTimer?.cancel()
    }
}