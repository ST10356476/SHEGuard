package com.iiest10356476.sheguard.ui.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.iiest10356476.sheguard.data.EmergencyContact

class LocationAndWhatsAppHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun sendLocationToContacts(
        contacts: List<EmergencyContact>,
        message: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasLocationPermission()) {
            onError("Location permission not granted")
            return
        }

        if (!isLocationEnabled()) {
            onError("Location services are disabled. Please enable GPS in your device settings.")
            return
        }

        // Show loading message
        getCurrentLocation { location ->
            val finalMessage = if (location != null) {
                val locationUrl = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
                "$message\n\n📍 My current location: $locationUrl\n\n🛡️ Sent via SHEGuard for your safety"
            } else {
                "$message\n\n⚠️ Location unavailable at the moment\n\n🛡️ Sent via SHEGuard for your safety"
            }
            sendWhatsAppMessages(contacts, finalMessage, onSuccess, onError)
        }
    }

    private fun getCurrentLocation(callback: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            callback(null)
            return
        }

        if (!isLocationEnabled()) {
            callback(null)
            return
        }

        try {
            // First try to get last known location
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null && isLocationRecent(location)) {
                        callback(location)
                    } else {
                        // Request fresh location if last known is too old or null
                        requestFreshLocation(callback)
                    }
                }
                .addOnFailureListener {
                    // If getting last location fails, request fresh location
                    requestFreshLocation(callback)
                }
        } catch (e: SecurityException) {
            callback(null)
        }
    }

    private fun requestFreshLocation(callback: (Location?) -> Unit)

    {
        if (!hasLocationPermission()) {
            callback(null)
            return
        }

        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                5000 // 5 seconds interval
            ).apply {
                setMinUpdateDistanceMeters(0f)
                setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                setWaitForAccurateLocation(false)
                setMaxUpdates(1)
                setMinUpdateIntervalMillis(2000) // Minimum 2 seconds
            }.build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this)
                    val location = locationResult.lastLocation
                    callback(location)
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            // Timeout after 10 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                fusedLocationClient.removeLocationUpdates(locationCallback)
                callback(null)
            }, 10000)

        } catch (e: SecurityException) {
            callback(null)
        } catch (e: Exception) {
            callback(null)
        }
    }

    private fun isLocationRecent(location: Location): Boolean {
        val threeMinutesAgo = System.currentTimeMillis() - (3 * 60 * 1000)
        return location.time > threeMinutesAgo
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun sendWhatsAppMessages(
        contacts: List<EmergencyContact>,
        message: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isWhatsAppInstalled()) {
            onError("WhatsApp is not installed")
            return
        }

        val selectedContacts = contacts.filter { it.isSelected }
        if (selectedContacts.isEmpty()) {
            onError("No contacts selected")
            return
        }

        try {
            // For multiple contacts, we'll open WhatsApp with the first contact
            // and the user can manually send to others
            val firstContact = selectedContacts.first()
            openWhatsAppChat(firstContact.getFormattedPhoneNumber(), message)

            if (selectedContacts.size > 1) {
                Toast.makeText(
                    context,
                    "WhatsApp opened for ${firstContact.name}. You can send to other contacts manually.",
                    Toast.LENGTH_LONG
                ).show()
            }

            onSuccess()
        } catch (e: Exception) {
            onError("Failed to open WhatsApp: ${e.message}")
        }
    }

    private fun openWhatsAppChat(phoneNumber: String, message: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://wa.me/$phoneNumber?text=${Uri.encode(message)}")
            setPackage("com.whatsapp")
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Try WhatsApp Business if regular WhatsApp is not available
            intent.setPackage("com.whatsapp.w4b")
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                throw Exception("WhatsApp not found")
            }
        }
    }

    private fun isWhatsAppInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.whatsapp", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            try {
                context.packageManager.getPackageInfo("com.whatsapp.w4b", 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        context.startActivity(intent)
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}