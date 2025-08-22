package com.iiest10356476.sheguard.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
import com.iiest10356476.sheguard.data.models.FileType
import com.iiest10356476.sheguard.data.repository.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PanicRecordingService : Service() {

    private var recorder: MediaRecorder? = null
    private var outputFile: String? = null
    private val TAG = "PanicRecordingService"

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        startRecording()
    }

    private fun startForegroundService() {
        val channelId = "panic_recording_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Panic Recording",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("SHEGuard")
            .setContentText("Recording triggered for safety")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        startForeground(1, notification)
    }

    private fun startRecording() {
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

            Log.d(TAG, "Recording started: $outputFile")

            // Stop automatically after 1 minute
            CoroutineScope(Dispatchers.IO).launch {
                kotlinx.coroutines.delay(60_000)
                stopRecording()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Recording failed", e)
            stopSelf()
        }
    }

    private fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            Log.d(TAG, "Recording stopped")

            outputFile?.let { uploadAndSaveToVault(it) }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recorder", e)
        } finally {
            stopSelf()
        }
    }

    // ✅ Single function to upload and save to Vault
    private fun uploadAndSaveToVault(filePath: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val ref = FirebaseStorage.getInstance().reference.child("panic_recordings/$uid/${File(filePath).name}")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ref.putFile(File(filePath).toUri()).await()
                val downloadUrl = ref.downloadUrl.await().toString()
                Log.d(TAG, "Upload successful: ${File(filePath).name}")

                // Save to Vault
                val vaultRepository = VaultRepository()
                vaultRepository.saveVaultFile(downloadUrl, FileType.AUDIO)

            } catch (e: Exception) {
                Log.e(TAG, "Upload failed", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { recorder?.release(); recorder = null } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
