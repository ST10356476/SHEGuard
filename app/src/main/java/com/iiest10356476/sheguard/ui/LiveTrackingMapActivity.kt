package com.iiest10356476.sheguard.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.services.LiveLocationTracker
import java.text.SimpleDateFormat
import java.util.*

class LiveTrackingMapActivity :  BaseActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var sessionIdText: TextView
    private lateinit var statusText: TextView
    private lateinit var lastUpdateText: TextView
    private lateinit var stopTrackingButton: Button
    private lateinit var shareLocationButton: Button
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var sessionId = ""
    private var isTracking = false
    private var trackingDuration = 0L
    private val locationHistory = mutableListOf<LatLng>()
    private val updateHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_tracking_map)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViews()
        setupMap()
        setupLocationClient()

        // Get session data from intent
        sessionId = intent.getStringExtra("session_id") ?: "SHE${kotlin.random.Random.nextInt(1000, 9999)}"
        isTracking = intent.getBooleanExtra("is_tracking", true)
        trackingDuration = intent.getLongExtra("duration", 2 * 60 * 60 * 1000L)

        updateUI()
        startLocationUpdates()
    }

    private fun setupViews() {
        sessionIdText = findViewById(R.id.session_id_text)
        statusText = findViewById(R.id.status_text)
        lastUpdateText = findViewById(R.id.last_update_text)
        stopTrackingButton = findViewById(R.id.stop_tracking_button)
        shareLocationButton = findViewById(R.id.share_location_button)

        // Back button
        findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            finish()
        }

        // Stop tracking button
        stopTrackingButton.setOnClickListener {
            stopLiveTracking()
        }

        // Share location button
        shareLocationButton.setOnClickListener {
            shareCurrentLocation()
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    updateLocationOnMap(location.latitude, location.longitude)
                }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Configure map
        googleMap.apply {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isMyLocationButtonEnabled = true
            uiSettings.isCompassEnabled = true
            mapType = GoogleMap.MAP_TYPE_NORMAL
        }

        // Try to enable location if permission granted
        if (hasLocationPermission()) {
            try {
                googleMap.isMyLocationEnabled = true
                getCurrentLocation()
            } catch (e: SecurityException) {
                // Handle permission not granted
            }
        }

        // Set initial location (Pretoria as default)
        val initialLocation = LatLng(-25.7479, 28.2293)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 15f))
    }

    private fun getCurrentLocation() {
        if (!hasLocationPermission()) return

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        val currentLatLng = LatLng(it.latitude, it.longitude)
                        updateLocationOnMap(it.latitude, it.longitude)
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
                    }
                }
        } catch (e: SecurityException) {
            // Handle permission error
        }
    }

    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            30000 // 30 seconds
        ).apply {
            setMinUpdateIntervalMillis(15000) // 15 seconds minimum
            setMinUpdateDistanceMeters(10f) // 10 meters
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Handle permission error
        }
    }

    private fun updateLocationOnMap(latitude: Double, longitude: Double) {
        if (!::googleMap.isInitialized) return

        val newLocation = LatLng(latitude, longitude)

        // Add to history if it's a significant distance from last point
        if (locationHistory.isEmpty() ||
            getDistance(locationHistory.last(), newLocation) > 10) {
            locationHistory.add(newLocation)
        }

        // Clear previous markers and polylines
        googleMap.clear()

        // Add current location marker
        googleMap.addMarker(
            MarkerOptions()
                .position(newLocation)
                .title("Current Location")
                .snippet("Updated: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}")
        )

        // Draw path if we have multiple points
        if (locationHistory.size > 1) {
            val polylineOptions = PolylineOptions()
                .addAll(locationHistory)
                .color(0xFF6A1B9A.toInt()) // Purple color
                .width(8f)
            googleMap.addPolyline(polylineOptions)
        }

        // Move camera to current location smoothly
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newLocation, 16f))

        // Update UI
        updateUI()
    }

    private fun getDistance(point1: LatLng, point2: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude,
            results
        )
        return results[0]
    }

    private fun updateUI() {
        sessionIdText.text = "Session: $sessionId"
        statusText.text = if (isTracking) "🟢 Live Tracking Active" else "🔴 Tracking Stopped"
        lastUpdateText.text = "Last update: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"

        stopTrackingButton.isEnabled = isTracking
        shareLocationButton.isEnabled = isTracking
    }

    private fun shareCurrentLocation() {
        if (!hasLocationPermission()) return

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        val locationUrl = "https://maps.google.com/?q=${it.latitude},${it.longitude}"
                        val shareText = "📍 My current location (via SHEGuard):\n$locationUrl\n\nSession: $sessionId"

                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        startActivity(Intent.createChooser(shareIntent, "Share Location"))
                    }
                }
        } catch (e: SecurityException) {
            // Handle permission error
        }
    }

    private fun stopLiveTracking() {
        val serviceIntent = Intent(this, LiveLocationTracker::class.java).apply {
            action = LiveLocationTracker.ACTION_STOP_TRACKING
        }
        startService(serviceIntent)

        isTracking = false
        updateUI()

        // Stop location updates
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // Show confirmation and finish
        android.app.AlertDialog.Builder(this)
            .setTitle("Tracking Stopped")
            .setMessage("Live location tracking has been stopped. Your emergency contacts have been notified that you're safe.")
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onResume() {
        super.onResume()
        if (isTracking && hasLocationPermission()) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        updateHandler.removeCallbacksAndMessages(null)
    }
}