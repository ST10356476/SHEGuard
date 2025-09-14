package com.iiest10356476.sheguard.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.util.Log
import com.iiest10356476.sheguard.services.PanicRecordingService

class RecordingController(private val context: Context) {

    companion object {
        private const val TAG = "RecordingController"
    }

    /**
     * Start panic recording
     */
    fun startRecording() {
        Log.d(TAG, "Starting panic recording")
        val intent = Intent(context, PanicRecordingService::class.java).apply {
            action = PanicRecordingService.ACTION_START_RECORDING
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    /**
     * Stop panic recording
     */
    fun stopRecording() {
        Log.d(TAG, "Stopping panic recording")
        val intent = Intent(context, PanicRecordingService::class.java).apply {
            action = PanicRecordingService.ACTION_STOP_RECORDING
        }
        context.startService(intent)
    }

    /**
     * Check if recording is currently active
     */
    fun isRecording(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        @Suppress("DEPRECATION")
        val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)

        return runningServices.any { serviceInfo ->
            serviceInfo.service.className == PanicRecordingService::class.java.name
        }
    }

    /**
     * Toggle recording state
     */
    fun toggleRecording() {
        if (isRecording()) {
            stopRecording()
        } else {
            startRecording()
        }
    }
}