package com.iiest10356476.sheguard.data.repository

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.iiest10356476.sheguard.data.models.PanicEvent
import java.util.UUID
import java.util.concurrent.Executors

class VideoRecordingService : LifecycleService() {

    private val executor = Executors.newSingleThreadExecutor()
    private var backRecording: Recording? = null

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val notification: Notification = NotificationCompat.Builder(this, "panic_channel")
            .setContentTitle("Recording Panic Event")
            .setContentText("Recording video for 1 minute...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Record for 1 minute (60,000 ms)
        val duration = 60_000L

        startRecording(CameraSelector.LENS_FACING_BACK)

        Handler(mainLooper).postDelayed({
            stopRecording()
        }, duration)

        return START_NOT_STICKY
    }


    private fun startRecording(lensFacing: Int) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val recorder = Recorder.Builder().build()
                val videoCapture = VideoCapture.withOutput(recorder)

                val selector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, selector, videoCapture)

                val name = "panic_${System.currentTimeMillis()}.mp4"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/PanicEvents")
                }

                val outputOptions = MediaStoreOutputOptions.Builder(
                    contentResolver,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                ).setContentValues(contentValues).build()

                // Permission check
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                ) {
                    backRecording = recorder
                        .prepareRecording(this, outputOptions)
                        .withAudioEnabled()
                        .start(executor) { event ->
                            if (event is VideoRecordEvent.Finalize) {
                                event.outputResults.outputUri.let { savedUri ->
                                    Log.d("PANIC", "Recording saved: $savedUri")
                                    savePanicEventToFirestore(savedUri.toString())
                                }
                            }
                        }

                    Log.d("PANIC", "Started recording for 1 minute")
                } else {
                    Log.e("PANIC", "Required permissions not granted")
                }

            } catch (exc: Exception) {
                Log.e("PANIC", "Failed to start recording: $exc")
            }
        }, executor)
    }

    private fun stopRecording() {
        backRecording?.stop()
        backRecording = null
    }

    private fun savePanicEventToFirestore(videoPath: String) {
        val panicEventId = UUID.randomUUID().toString()
        val uid = auth.currentUser?.uid ?: "anonymous"

        val panicEvent = PanicEvent(
            panicEventId = panicEventId,
            recordUrl = videoPath, // local URI, not Firebase
            eventDate = System.currentTimeMillis(),
            latitude = 0.0,   // TODO: add GPS Through API
            longitude = 0.0,
            uid = uid
        )

        firestore.collection("panicEvents").document(panicEventId)
            .set(panicEvent)
            .addOnSuccessListener {
                Log.d("PANIC", "Saved panic event with video path: $videoPath")
            }
            .addOnFailureListener { e ->
                Log.e("PANIC", "Error saving panic event", e)
            }
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "panic_channel",
                "Panic Event Recording",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
