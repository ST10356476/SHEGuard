package com.iiest10356476.sheguard.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VolumeButtonService : AccessibilityService() {

    companion object {
        private const val TAG = "VolumeButtonService"
        private const val TRIGGER_COUNT = 3
        private const val RESET_DELAY = 3000L
    }

    private var volumePressCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var panicService: PanicService

    private val resetRunnable = Runnable {
        volumePressCount = 0
        Log.d(TAG, "Volume counter reset")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Initialize panic service
        panicService = PanicService(this)

        // Configure accessibility service to capture key events
        serviceInfo = serviceInfo.apply {
            flags = serviceInfo.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }

        Log.d(TAG, "VolumeButtonService connected")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Only handle volume down key press events
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            volumePressCount++
            Log.d(TAG, "Volume down pressed: $volumePressCount/$TRIGGER_COUNT")

            // Reset the counter after delay if no more presses
            handler.removeCallbacks(resetRunnable)
            handler.postDelayed(resetRunnable, RESET_DELAY)

            // Trigger panic mode when threshold reached
            if (volumePressCount >= TRIGGER_COUNT) {
                volumePressCount = 0
                handler.removeCallbacks(resetRunnable)
                triggerSilentPanic()
                return true // Consume the event to prevent volume change
            }
        }

        return super.onKeyEvent(event)
    }

    private fun triggerSilentPanic() {
        Log.i(TAG, "Silent panic triggered via volume keys")

        // Use coroutine to handle async panic operations
        CoroutineScope(Dispatchers.IO).launch {
            try {
                panicService.triggerVolumeKeyPanic()
                Log.d(TAG, "Silent panic mode completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error in silent panic mode", e)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Required override - no implementation needed for key events
    }

    override fun onInterrupt() {
        Log.d(TAG, "VolumeButtonService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(resetRunnable)
        Log.d(TAG, "VolumeButtonService destroyed")
    }
}