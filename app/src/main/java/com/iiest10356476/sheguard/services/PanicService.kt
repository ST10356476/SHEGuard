package com.iiest10356476.sheguard.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.iiest10356476.sheguard.utils.ContactsPreferencesHelper
import com.iiest10356476.sheguard.utils.RecordingController
import kotlinx.coroutines.tasks.await

class PanicService(private val context: Context) {

    companion object {
        private const val TAG = "PanicService"
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val recordingController = RecordingController(context)

    suspend fun triggerPanicMode(includeAudioRecording: Boolean = true) {
        try {
            Log.d(TAG, "Panic mode triggered! Audio recording: $includeAudioRecording")

            // Start audio recording first (if enabled and has permission)
            if (includeAudioRecording && hasAudioPermission()) {
                recordingController.startRecording()
            }

            val currentLocation = getCurrentLocation()

            // Get emergency contacts from TrackingEventOperation's saved contacts
            val emergencyContacts = getTrackingEmergencyContacts()

            // Send panic alert to emergency contacts
            sendPanicSMS(emergencyContacts, currentLocation, includeAudioRecording)

            // Log panic event to Firebase
            logPanicEvent(currentLocation, includeAudioRecording)

        } catch (e: Exception) {
            Log.e(TAG, "Error in panic mode", e)
            throw e
        }
    }

    suspend fun triggerVolumeKeyPanic() {
        try {
            Log.d(TAG, "Volume key panic triggered")

            // Always include audio recording for volume key trigger (stealth mode)
            if (hasAudioPermission()) {
                recordingController.startRecording()
            }

            val currentLocation = getCurrentLocation()
            val emergencyContacts = getTrackingEmergencyContacts()

            sendPanicSMS(emergencyContacts, currentLocation, true)

            // Log with different trigger method
            val userId = auth.currentUser?.uid ?: return
            val panicEvent = hashMapOf(
                "userId" to userId,
                "timestamp" to System.currentTimeMillis(),
                "latitude" to currentLocation?.latitude,
                "longitude" to currentLocation?.longitude,
                "type" to "volume_key_panic",
                "audioRecording" to true,
                "triggerMethod" to "volume_keys"
            )

            firestore.collection("panic_events")
                .add(panicEvent)
                .await()

        } catch (e: Exception) {
            Log.e(TAG, "Error in volume key panic", e)
            throw e
        }
    }

    /**
     * Get emergency contacts from ContactsPreferencesHelper
     * This is the same storage used by TrackingEventOperationActivity
     */
    private fun getTrackingEmergencyContacts(): List<String> {
        return try {
            val contactsHelper = ContactsPreferencesHelper(context)
            val emergencyContacts = contactsHelper.getContacts()

            // Convert EmergencyContact objects to phone number strings
            val phoneNumbers = emergencyContacts.map { contact ->
                contact.phoneNumber.replace(Regex("[^+\\d]"), "") // Clean phone number
            }.filter { it.isNotEmpty() }

            Log.d(TAG, "Retrieved ${phoneNumbers.size} emergency contacts from tracking storage")
            phoneNumbers

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tracking emergency contacts", e)
            emptyList()
        }
    }

    /**
     * Stop any ongoing recording
     */
    fun stopRecording() {
        Log.d(TAG, "Stopping recording from PanicService")
        recordingController.stopRecording()
    }

    /**
     * Check if recording is currently active
     */
    fun isRecording(): Boolean {
        return recordingController.isRecording()
    }

    private fun hasAudioPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getCurrentLocation(): Location? {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted")
            return null
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting location", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location", e)
            null
        }
    }

    private fun sendPanicSMS(contacts: List<String>, location: Location?, audioRecording: Boolean) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "SMS permission not granted")
            return
        }

        if (contacts.isEmpty()) {
            Log.w(TAG, "No emergency contacts available for panic SMS")
            return
        }

        val locationText = location?.let {
            "Location: https://maps.google.com/?q=${it.latitude},${it.longitude}"
        } ?: "Location unavailable"

        val audioText = if (audioRecording) "\nAudio recording started for evidence." else ""

        val message = "🚨 EMERGENCY ALERT 🚨\n" +
                "This is an automated emergency message from SHEGuard app.\n" +
                "I need immediate help!\n" +
                locationText + audioText + "\n" +
                "Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}"

        try {
            val smsManager = SmsManager.getDefault()

            contacts.forEach { phoneNumber ->
                try {
                    val parts = smsManager.divideMessage(message)
                    if (parts.size == 1) {
                        smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                    } else {
                        smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
                    }
                    Log.d(TAG, "Panic SMS sent to: $phoneNumber")
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException sending SMS to $phoneNumber", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send SMS to $phoneNumber", e)
                }
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException with SMS manager", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending panic SMS", e)
        }
    }

    private suspend fun logPanicEvent(location: Location?, audioRecording: Boolean) {
        try {
            val userId = auth.currentUser?.uid ?: return

            val panicEvent = hashMapOf(
                "userId" to userId,
                "timestamp" to System.currentTimeMillis(),
                "latitude" to location?.latitude,
                "longitude" to location?.longitude,
                "type" to "panic_button_triggered",
                "audioRecording" to audioRecording,
                "triggerMethod" to "button"
            )

            firestore.collection("panic_events")
                .add(panicEvent)
                .await()

            Log.d(TAG, "Panic event logged to Firebase")

        } catch (e: Exception) {
            Log.e(TAG, "Error logging panic event", e)
        }
    }
}