package com.iiest10356476.sheguard.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.data.EmergencyContact
import com.iiest10356476.sheguard.utils.ContactsPreferencesHelper
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class LiveLocationTracker : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var contactsHelper: ContactsPreferencesHelper

    private var isTracking = false
    private var trackingDuration = 0L // in milliseconds
    private var sessionId = ""
    private var lastSentTime = 0L
    private var customMessage = ""
    private val locationUpdateInterval = 60000L // 1 minute

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "SHEGuard_Location_Channel"
        const val ACTION_START_TRACKING = "start_tracking"
        const val ACTION_STOP_TRACKING = "stop_tracking"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_MESSAGE = "message"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        contactsHelper = ContactsPreferencesHelper(this)
        createNotificationChannel()
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                val duration = intent.getLongExtra(EXTRA_DURATION, 2 * 60 * 60 * 1000) // 2 hours default
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "I'm sharing my live location with you for safety."
                startLocationTracking(duration, message)
            }
            ACTION_STOP_TRACKING -> {
                stopLocationTracking()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SHEGuard Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when SHEGuard is tracking your location for safety"
                setSound(null, null)
                enableVibration(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                val location = locationResult.lastLocation
                if (location != null) {
                    handleLocationUpdate(location)
                }
            }
        }
    }

    private fun startLocationTracking(duration: Long, message: String) {
        if (isTracking) return

        if (!hasLocationPermission()) {
            stopSelf()
            return
        }

        isTracking = true
        trackingDuration = duration
        customMessage = message
        sessionId = generateSessionId()
        lastSentTime = 0L

        // Send initial message to contacts
        sendInitialMessage(message)

        // Start location updates
        startLocationUpdates()

        // Show persistent notification
        startForeground(NOTIFICATION_ID, createTrackingNotification())

        // Set timer to stop tracking after duration
        android.os.Handler(Looper.getMainLooper()).postDelayed({
            if (isTracking) {
                stopLocationTracking()
            }
        }, duration)
    }

    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            30000 // 30 seconds
        ).apply {
            setMinUpdateDistanceMeters(50f) // Update every 50 meters
            setMinUpdateIntervalMillis(15000) // Minimum 15 seconds
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(false)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            stopLocationTracking()
        }
    }

    private fun handleLocationUpdate(location: Location) {
        val currentTime = System.currentTimeMillis()

        // Send location update to contacts every minute or if significant distance change
        if (currentTime - lastSentTime >= locationUpdateInterval) {
            sendLocationUpdate(location)
            lastSentTime = currentTime
        }

        // Update notification with current location
        updateNotification(location)
    }

    private fun sendInitialMessage(customMessage: String) {
        val contacts = contactsHelper.getSelectedContacts()
        if (contacts.isNotEmpty()) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val message = """
                🛡️ SHEGuard Live Tracking Started
                
                $customMessage
                
                I've started live location sharing at ${timeFormat.format(Date())}.
                You'll receive location updates every minute.
                
                Session ID: $sessionId
                
                If you don't hear from me, please check on me. 💜
            """.trimIndent()

            contacts.forEach { contact ->
                sendWhatsAppMessage(contact, message)
            }
        }
    }

    private fun sendLocationUpdate(location: Location) {
        val contacts = contactsHelper.getSelectedContacts()
        if (contacts.isNotEmpty()) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val locationUrl = "https://maps.google.com/?q=${location.latitude},${location.longitude}"

            val message = """
                📍 Location Update - ${timeFormat.format(Date())}
                
                Current location: $locationUrl
                
                Session: $sessionId
                Still safe and checking in! 🛡️
            """.trimIndent()

            // Send to first contact to avoid spamming all contacts
            sendWhatsAppMessage(contacts.first(), message)
        }
    }

    private fun sendWhatsAppMessage(contact: EmergencyContact, message: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/${contact.getFormattedPhoneNumber()}?text=${Uri.encode(message)}")
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // Try WhatsApp Business
                intent.setPackage("com.whatsapp.w4b")
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                }
            }
        } catch (e: Exception) {
            // Handle error silently in background service
        }
    }

    private fun stopLocationTracking() {
        if (!isTracking) return

        isTracking = false

        // Stop location updates
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // Send final "safe" message
        sendSafeMessage()

        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun sendSafeMessage() {
        val contacts = contactsHelper.getSelectedContacts()
        if (contacts.isNotEmpty()) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val message = """
                ✅ SHEGuard Session Ended
                
                I've safely completed my journey at ${timeFormat.format(Date())}.
                
                Session: $sessionId
                
                Thank you for keeping me safe! 💜🛡️
            """.trimIndent()

            contacts.forEach { contact ->
                sendWhatsAppMessage(contact, message)
            }
        }
    }

    private fun createTrackingNotification(): Notification {
        val stopIntent = Intent(this, LiveLocationTracker::class.java).apply {
            action = ACTION_STOP_TRACKING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationCompat.Builder(this, CHANNEL_ID)
        } else {
            NotificationCompat.Builder(this)
        }

        return builder
            .setContentTitle("SHEGuard - Location Tracking Active")
            .setContentText("Sharing your location with emergency contacts")
            .setSmallIcon(R.drawable.ic_location_on_24)
            .setOngoing(true)
            .addAction(R.drawable.ic_stop_24, "Stop Tracking", stopPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSound(null)
            .setVibrate(null)
            .build()
    }

    private fun updateNotification(location: Location) {
        val builder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationCompat.Builder(this, CHANNEL_ID)
        } else {
            NotificationCompat.Builder(this)
        }

        val notification = builder
            .setContentTitle("SHEGuard - Location Tracking Active")
            .setContentText("Last update: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}")
            .setSmallIcon(R.drawable.ic_location_on_24)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSound(null)
            .setVibrate(null)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun generateSessionId(): String {
        return "SHE${Random.nextInt(1000, 9999)}"
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
}