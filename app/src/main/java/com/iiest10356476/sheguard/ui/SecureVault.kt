package com.iiest10356476.sheguard.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.data.repository.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SecureVault : AppCompatActivity() {

    private val vaultRepo = VaultRepository()
    private val selectedUris = mutableListOf<Uri>()

    // Permission launcher
    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val granted = perms.entries.all { it.value }
            if (granted) openFilePicker()
            else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }

    // File picker launcher
    private val pickFilesLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (!uris.isNullOrEmpty()) {
                selectedUris.clear()
                selectedUris.addAll(uris)
                uploadSelectedFiles()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_secure_vault)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val uploadButton = findViewById<Button>(R.id.upload_button)
        uploadButton.setOnClickListener {
            checkPermissionsAndPickFiles()
        }
    }

    private fun checkPermissionsAndPickFiles() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        requestStoragePermission.launch(permissions.toTypedArray())
    }

    private fun openFilePicker() {
        pickFilesLauncher.launch("*/*")
    }

    private fun uploadSelectedFiles() {
        val photoStrings = selectedUris.map { it.toString() }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                vaultRepo.uploadVault(
                    photos = photoStrings,
                    videos = listOf(),
                    audios = listOf()
                )
                runOnUiThread {
                    Toast.makeText(this@SecureVault, "Upload successful!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@SecureVault, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
