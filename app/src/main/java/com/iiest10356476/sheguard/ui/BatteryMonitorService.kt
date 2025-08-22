package com.iiest10356476.sheguard.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import com.iiest10356476.sheguard.data.EmergencyContact
import com.iiest10356476.sheguard.ui.utils.LocationAndWhatsAppHelper

class BatteryMonitorService : Service() {

    private lateinit var batteryReceiver: BroadcastReceiver
    private lateinit var locationHelper: LocationAndWhatsAppHelper
    private var batteryAlertSent = false

    override fun onCreate() {
        super.onCreate()
        locationHelper = LocationAndWhatsAppHelper(this)
        setupBatteryReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Restart service if killed
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupBatteryReceiver() {
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_BATTERY_LOW -> {
                        if (!batteryAlertSent) {
                            sendLowBatteryAlert()
                            batteryAlertSent = true
                        }
                    }
                    Intent.ACTION_BATTERY_OKAY -> {
                        batteryAlertSent = false // Reset flag when battery is okay
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
        }

        registerReceiver(batteryReceiver, filter)
    }

    private fun sendLowBatteryAlert() {
        // Get emergency contacts from preferences/database
        val emergencyContacts = getEmergencyContacts()

        if (emergencyContacts.isNotEmpty()) {
            val message = "⚠️ LOW BATTERY ALERT ⚠️\n\nMy phone battery is running low. Please check on me if you don't hear from me soon.\n\nThis is an automated safety message from SHEGuard."

            locationHelper.sendLocationToContacts(
                contacts = emergencyContacts,
                message = message,
                onSuccess = {
                    // Log success
                },
                onError = { error ->
                    // Log error
                }
            )
        }
    }

    private fun getEmergencyContacts(): List<EmergencyContact> {
        // In a real app, retrieve from SharedPreferences or database
        // For now, return empty list
        return emptyList()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
    }
}