package com.iiest10356476.sheguard.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.data.models.FileType
import com.iiest10356476.sheguard.data.repository.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PanicRecordingService : Service() {

    companion object {
        private const val TAG = "PanicRecordingService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "panic_recording_channel"

        // Actions for controlling the service
        const val ACTION_STOP_RECORDING = "com.iiest10356476.sheguard.STOP_RECORDING"
        const val ACTION_START_RECORDING = "com.iiest10356476.sheguard.START_RECORDING"

        // Recording duration options
        const val DEFAULT_RECORDING_DURATION = 60_000L // 1 minute
        const val EXTENDED_RECORDING_DURATION = 300_000L // 5 minutes
    }

    private var recorder: MediaRecorder? = null
    private var outputFile: String? = null
    private var autoStopJob: Job? = null
    private var isRecording = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PanicRecordingService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_RECORDING -> {
                Log.d(TAG, "Stop recording action received")
                stopRecordingAndFinish()
            }
            ACTION_START_RECORDING -> {
                Log.d(TAG, "Start recording action received")
                if (!isRecording) {
                    startForegroundService()
                    startRecording()
                }
            }
            else -> {
                // Default behavior - start recording
                startForegroundService()
                startRecording()
            }
        }

        return START_NOT_STICKY // Don't restart if killed
    }

    private fun startForegroundService() {
        createNotificationChannel()

        val stopIntent = Intent(this, PanicRecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Emergency Recording Active")
            .setContentText("Recording for safety - Tap to stop")
            .setSmallIcon(R.drawable.microphone) // Replace with your microphone icon
            .addAction(R.drawable.ic_stop_24, "Stop Recording", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Emergency Recording",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for emergency audio recording"
                enableVibration(false) // Silent for stealth mode
                setSound(null, null)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startRecording() {
        if (isRecording) {
            Log.w(TAG, "Recording already in progress")
            return
        }

        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "panic_$timestamp.m4a")
            outputFile = file.absolutePath

            recorder = MediaRecorder().apply {
                reset()
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile)
                prepare()
                start()
            }

            isRecording = true
            Log.d(TAG, "Recording started: $outputFile")

            // Auto-stop after default duration
            autoStopJob = CoroutineScope(Dispatchers.IO).launch {
                kotlinx.coroutines.delay(DEFAULT_RECORDING_DURATION)
                if (isRecording) {
                    Log.d(TAG, "Auto-stopping recording after timeout")
                    stopRecordingAndFinish()
                }
            }

            updateNotification("Recording... Tap to stop")

        } catch (e: Exception) {
            Log.e(TAG, "Recording failed", e)
            isRecording = false
            stopSelf()
        }
    }

    private fun updateNotification(text: String) {
        val stopIntent = Intent(this, PanicRecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Emergency Recording")
            .setContentText(text)
            .setSmallIcon(R.drawable.microphone)
            .addAction(R.drawable.ic_stop_24, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun stopRecordingAndFinish() {
        Log.d(TAG, "Stopping recording")

        autoStopJob?.cancel()

        try {
            recorder?.apply {
                if (isRecording) {
                    stop()
                }
                release()
            }
            recorder = null
            isRecording = false

            Log.d(TAG, "Recording stopped successfully")

            outputFile?.let { filePath ->
                updateNotification("Uploading recording...")
                uploadAndSaveToVault(filePath)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recorder", e)
            stopSelf()
        }
    }

    private fun uploadAndSaveToVault(filePath: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val fileName = File(filePath).name
        val ref = FirebaseStorage.getInstance().reference.child("panic_recordings/$uid/$fileName")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateNotification("Uploading... Please wait")

                ref.putFile(File(filePath).toUri()).await()
                val downloadUrl = ref.downloadUrl.await().toString()
                Log.d(TAG, "Upload successful: $fileName")

                // Save to Vault
                val vaultRepository = VaultRepository()
                vaultRepository.saveVaultFile(downloadUrl, FileType.AUDIO)

                Log.d(TAG, "Recording saved to vault successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Upload failed", e)
            } finally {
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "PanicRecordingService destroyed")

        autoStopJob?.cancel()

        try {
            if (isRecording) {
                recorder?.stop()
            }
            recorder?.release()
            recorder = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing recorder", e)
        }

        isRecording = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}