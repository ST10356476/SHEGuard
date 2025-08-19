package com.iiest10356476.sheguard.data.repository

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.iiest10356476.sheguard.data.models.PanicEvent

import java.util.UUID

class PanicEventRepository : AccessibilityService() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val panicEventsCollection = firestore.collection("PanicEvent")
    private var volumeUpPressed = false
    private var volumeDownPressed = false

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    volumeUpPressed = true
                    checkTrigger()
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    volumeDownPressed = true
                    checkTrigger()
                }
            }
        }
        return super.onKeyEvent(event)
    }

    private fun checkTrigger() {
        if (volumeUpPressed && volumeDownPressed) {
            Log.d("PANIC", "Panic Event Triggered")
            createAndSavePanicEvent()
            startRecording()

            //make the Vol UP & Down Back to default
            volumeUpPressed = false
            volumeDownPressed = false
        }
    }

    private fun createAndSavePanicEvent() {
        val panicEventId = UUID.randomUUID().toString()
        val uid = auth.currentUser?.uid ?: "anonymous"

        val timestamp = System.currentTimeMillis()

        val PanicEvent = PanicEvent(
            panicEventId = panicEventId,
            recordUrl = "Video Recording Started",  // placeholder (replace with actual Firebase video URL later)
            eventDate = timestamp,
            latitude = 0.0,   // TODO: get real GPS location
            longitude = 0.0,  // TODO: get real GPS location
            uid = uid
        )

        panicEventsCollection.document(panicEventId).set(PanicEvent)
            .addOnSuccessListener {
                Log.d("PANIC", "Panic event saved in Firestore ")
            }
            .addOnFailureListener { e ->
                Log.e("PANIC", "Error saving panic event ", e)
            }
    }

    private fun startRecording() {
        // Launch the foreground service to record the front hand back
        val intent = Intent(this, VideoRecordingService::class.java)
        intent.putExtra("DURATION", 5 * 60 * 1000L) // Todo : (5 * 60 * 1000L) = 5 mins, Remember to change it
        startForegroundService(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

    }

    override fun onInterrupt() {

    }
}
