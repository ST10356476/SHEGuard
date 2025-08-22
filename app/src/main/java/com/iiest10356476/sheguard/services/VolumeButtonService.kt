package com.iiest10356476.sheguard.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.iiest10356476.sheguard.ui.TransparentActivity

class VolumeButtonService : AccessibilityService() {

    private var volumePressCount = 0
    private val TRIGGER_COUNT = 8
    private val RESET_DELAY = 3000L
    private val handler = Handler(Looper.getMainLooper())

    private val resetRunnable = Runnable {
        volumePressCount = 0
        Log.i("VolumeButtonService", "Volume counter reset")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            volumePressCount++
            handler.removeCallbacks(resetRunnable)
            handler.postDelayed(resetRunnable, RESET_DELAY)

            if (volumePressCount >= TRIGGER_COUNT) {
                volumePressCount = 0
                Log.i("VolumeButtonService", "Triggering TransparentActivity")

                val intent = Intent(this, TransparentActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            }
            return true
        }
        return super.onKeyEvent(event)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            flags = serviceInfo.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
