package com.iiest10356476.sheguard.ui

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.adapters.EmergencyContactAdapter
import com.iiest10356476.sheguard.data.EmergencyContact
import com.iiest10356476.sheguard.services.LiveLocationTracker
import com.iiest10356476.sheguard.ui.utils.LocationAndWhatsAppHelper
import com.iiest10356476.sheguard.utils.ContactsPreferencesHelper
import java.util.UUID

class TrackingEventOperationActivity : AppCompatActivity() {

    private lateinit var contactsAdapter: EmergencyContactAdapter
    private lateinit var contactsRecyclerView: RecyclerView
    private lateinit var customMessageInput: EditText
    private lateinit var locationHelper: LocationAndWhatsAppHelper

    private val emergencyContacts = mutableListOf<EmergencyContact>()
    private var selectedMessage = ""

    // Preset messages
    private val presetMessages = mapOf(
        "going_out" to "Hi! I'm going out for the evening. I'll share my location with you for safety. Please check on me if you don't hear from me by [time]. 🛡️",
        "working_late" to "Hey! Working late tonight. Sharing my location for safety. Expected to finish around [time]. 🏢",
        "traveling" to "Hi! I'm traveling today. Sharing my location for your peace of mind. Will update you when I reach safely. ✈️",
        "on_date" to "Going on a date tonight! Sharing my location with you for safety. Will text you when I'm home safe. 💕"
    )

    // Permission launcher for location and contacts
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
            }
            permissions[Manifest.permission.READ_CONTACTS] == true -> {
                Toast.makeText(this, "Contacts permission granted", Toast.LENGTH_SHORT).show()
                loadContactsFromPhone()
            }
            else -> {
                Toast.makeText(this, "Permissions are required for full functionality", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tracking_event_operation)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        checkPermissions()
        loadSavedEmergencyContacts()
    }

    private fun initializeViews() {
        locationHelper = LocationAndWhatsAppHelper(this)
        contactsRecyclerView = findViewById(R.id.contacts_recycler_view)
        customMessageInput = findViewById(R.id.custom_message_input)
    }

    private fun setupRecyclerView() {
        contactsAdapter = EmergencyContactAdapter(emergencyContacts) { contact, isSelected ->
            updateContactSelection(contact, isSelected)
        }

        contactsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@TrackingEventOperationActivity)
            adapter = contactsAdapter
        }
    }

    private fun setupClickListeners() {
        // Back button
        findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            finish()
        }

        // Add contact button - shows phone contacts
        findViewById<Button>(R.id.add_contact_button).setOnClickListener {
            if (hasContactsPermission()) {
                loadContactsFromPhone()
            } else {
                showAddContactDialog() // Fallback to manual entry
            }
        }

        // Preset message buttons
        findViewById<Button>(R.id.going_out_button).setOnClickListener {
            selectPresetMessage("going_out")
        }

        findViewById<Button>(R.id.working_late_button).setOnClickListener {
            selectPresetMessage("working_late")
        }

        findViewById<Button>(R.id.traveling_button).setOnClickListener {
            selectPresetMessage("traveling")
        }

        findViewById<Button>(R.id.on_date_button).setOnClickListener {
            selectPresetMessage("on_date")
        }

        // Action buttons
        findViewById<Button>(R.id.cancel_button).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.send_whatsapp_button).setOnClickListener {
            sendLocationToContacts()
        }
    }

    private fun loadSavedEmergencyContacts() {
        val contactsHelper = ContactsPreferencesHelper(this)
        val savedContacts = contactsHelper.getContacts()

        emergencyContacts.clear()
        emergencyContacts.addAll(savedContacts)
        contactsAdapter.notifyDataSetChanged()
    }

    private fun loadContactsFromPhone() {
        if (!hasContactsPermission()) {
            Toast.makeText(this, "Contacts permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        val phoneContacts = getPhoneContacts()
        if (phoneContacts.isNotEmpty()) {
            showContactSelectionDialog(phoneContacts)
        } else {
            Toast.makeText(this, "No contacts found on your phone", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getPhoneContacts(): List<EmergencyContact> {
        val contacts = mutableListOf<EmergencyContact>()

        try {
            val cursor: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val name = if (nameIndex >= 0) it.getString(nameIndex) ?: "" else ""
                    val phoneNumber = if (numberIndex >= 0) it.getString(numberIndex) ?: "" else ""

                    if (name.isNotEmpty() && phoneNumber.isNotEmpty()) {
                        val contact = EmergencyContact(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            phoneNumber = phoneNumber.replace(Regex("[^+\\d]"), ""),
                            isSelected = false
                        )

                        if (!contacts.any { existing -> existing.phoneNumber == contact.phoneNumber }) {
                            contacts.add(contact)
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permission denied to read contacts", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error reading contacts", Toast.LENGTH_SHORT).show()
        }

        return contacts
    }

    private fun showContactSelectionDialog(phoneContacts: List<EmergencyContact>) {
        val contactNames = phoneContacts.map { "${it.name} (${it.phoneNumber})" }.toTypedArray()
        val checkedItems = BooleanArray(contactNames.size) { false }

        AlertDialog.Builder(this)
            .setTitle("Select Emergency Contacts")
            .setMultiChoiceItems(contactNames, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("Add Selected") { _, _ ->
                val selectedContacts = phoneContacts.filterIndexed { index, _ ->
                    checkedItems[index]
                }

                if (selectedContacts.isNotEmpty()) {
                    addContactsToEmergencyList(selectedContacts)
                    Toast.makeText(this, "${selectedContacts.size} contacts added", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addContactsToEmergencyList(contacts: List<EmergencyContact>) {
        val contactsHelper = ContactsPreferencesHelper(this)

        contacts.forEach { contact ->
            val exists = emergencyContacts.any { it.phoneNumber == contact.phoneNumber }
            if (!exists) {
                emergencyContacts.add(contact)
                contactsHelper.addContact(contact)
            }
        }

        contactsAdapter.notifyDataSetChanged()
    }

    private fun updateContactSelection(contact: EmergencyContact, isSelected: Boolean) {
        val index = emergencyContacts.indexOfFirst { it.id == contact.id }
        if (index != -1) {
            emergencyContacts[index] = contact.copy(isSelected = isSelected)

            val contactsHelper = ContactsPreferencesHelper(this)
            contactsHelper.updateContact(emergencyContacts[index])
        }
    }

    private fun selectPresetMessage(messageKey: String) {
        selectedMessage = presetMessages[messageKey] ?: ""
        customMessageInput.setText(selectedMessage)

        Toast.makeText(this, "Message template selected", Toast.LENGTH_SHORT).show()
    }

    private fun showAddContactDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_contact, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.contact_name_input)
        val phoneInput = dialogView.findViewById<EditText>(R.id.contact_phone_input)

        AlertDialog.Builder(this)
            .setTitle("Add Emergency Contact")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()

                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    val newContact = EmergencyContact(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        phoneNumber = phone,
                        isSelected = false
                    )
                    contactsAdapter.addContact(newContact)
                    emergencyContacts.add(newContact)

                    val contactsHelper = ContactsPreferencesHelper(this)
                    contactsHelper.addContact(newContact)

                    Toast.makeText(this, "Contact added successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendLocationToContacts() {
        val message = if (customMessageInput.text.toString().trim().isNotEmpty()) {
            customMessageInput.text.toString().trim()
        } else {
            "Hi! I'm sharing my live location with you for safety. Please check on me if needed. 🛡️"
        }

        val selectedContacts = emergencyContacts.filter { it.isSelected }
        if (selectedContacts.isEmpty()) {
            Toast.makeText(this, "Please select at least one contact", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isLocationEnabled()) {
            showLocationEnableDialog()
            return
        }

        showDurationSelectionDialog(message)
    }

    private fun showDurationSelectionDialog(message: String) {
        val durations = arrayOf(
            "30 minutes",
            "1 hour",
            "2 hours",
            "4 hours",
            "8 hours",
            "Until I stop manually"
        )

        val durationValues = arrayOf(
            30 * 60 * 1000L,      // 30 minutes
            60 * 60 * 1000L,      // 1 hour
            2 * 60 * 60 * 1000L,  // 2 hours
            4 * 60 * 60 * 1000L,  // 4 hours
            8 * 60 * 60 * 1000L,  // 8 hours
            24 * 60 * 60 * 1000L  // 24 hours (manual stop)
        )

        AlertDialog.Builder(this)
            .setTitle("How long should we track your location?")
            .setItems(durations) { _, which ->
                startLiveLocationTracking(durationValues[which], message)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startLiveLocationTracking(duration: Long, message: String) {
        val serviceIntent = Intent(this, LiveLocationTracker::class.java).apply {
            action = LiveLocationTracker.ACTION_START_TRACKING
            putExtra(LiveLocationTracker.EXTRA_DURATION, duration)
            putExtra(LiveLocationTracker.EXTRA_MESSAGE, message)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        Toast.makeText(this, "Live location sharing started!", Toast.LENGTH_LONG).show()

        val sessionId = "SHE${kotlin.random.Random.nextInt(1000, 9999)}"

        val mapIntent = Intent(this, LiveTrackingMapActivity::class.java).apply {
            putExtra("session_id", sessionId)
            putExtra("is_tracking", true)
            putExtra("duration", duration)
        }
        startActivity(mapIntent)

        finish()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }

    private fun showLocationEnableDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Location Services")
            .setMessage("Location services are required to share your location with emergency contacts. Would you like to enable them?")
            .setPositiveButton("Enable") { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Send Without Location") { _, _ ->
                sendMessageWithoutLocation()
            }
            .setCancelable(false)
            .show()
    }

    private fun sendMessageWithoutLocation() {
        val message = if (customMessageInput.text.toString().trim().isNotEmpty()) {
            customMessageInput.text.toString().trim()
        } else {
            "Hi! I'm sharing my safety status with you. Please check on me if needed. 🛡️"
        }

        val selectedContacts = emergencyContacts.filter { it.isSelected }
        val finalMessage = "$message\n\n⚠️ Location services unavailable\n\n🛡️ Sent via SHEGuard for your safety"

        if (selectedContacts.isNotEmpty()) {
            try {
                val firstContact = selectedContacts.first()
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("https://wa.me/${firstContact.getFormattedPhoneNumber()}?text=${android.net.Uri.encode(finalMessage)}")
                    setPackage("com.whatsapp")
                }

                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                    Toast.makeText(this, "Message sent without location", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "WhatsApp not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (!hasLocationPermission()) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (!hasContactsPermission()) {
            permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            if (!isLocationEnabled()) {
                Toast.makeText(this, "Please enable location services for better functionality", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }
}