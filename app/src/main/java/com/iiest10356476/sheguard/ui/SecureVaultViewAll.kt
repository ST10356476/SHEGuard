package com.iiest10356476.sheguard.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log

import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.data.models.FileType
import com.iiest10356476.sheguard.data.models.Vault
import com.iiest10356476.sheguard.data.models.VaultFile

import com.iiest10356476.sheguard.data.repository.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext


class SecureVaultViewAll : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

    private lateinit var vaultAdapter: VaultAdapter
    private val vaultRepository = VaultRepository()
    private var vaultItems: List<Vault> = emptyList()
    private var currentFilter: FileType? = null

    // Filter buttons
    private lateinit var filterAllButton: Button
    private lateinit var filterPhotosButton: Button
    private lateinit var filterVideosButton: Button
    private lateinit var filterAudioButton: Button
    private lateinit var filterDocumentsButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_secure_vault_view_all)


        initializeViews()
        setupClickListeners()
        setupRecyclerView()
        loadVaultItems()

        recyclerView = findViewById(R.id.files_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)


        // Apply system insets (status/nav bar padding)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loadVaultItems()
    }

    private fun loadVaultItems() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        CoroutineScope(Dispatchers.Main).launch {
            vaultItems = vaultRepository.getRecentVaultItems(uid)
            recyclerView.adapter = VaultAdapter(
                vaultItems,
                onDeleteClick = { file ->
                    CoroutineScope(Dispatchers.Main).launch {
                        vaultRepository.deleteFile(file)
                        loadVaultItems()
                    }
                },
                onDownloadClick = { file ->
                    vaultRepository.getDownloadUrl(file) { url ->
                        url?.let { Log.d("SecureVaultViewAll", "Download URL: $it") }
                    }
                }
            )
        }
    }


    private fun initializeViews() {
        recyclerView = findViewById(R.id.files_recycler_view)

        // Filter buttons
        filterAllButton = findViewById(R.id.filter_all_button)
        filterPhotosButton = findViewById(R.id.filter_photos_button)
        filterVideosButton = findViewById(R.id.filter_videos_button)
        filterAudioButton = findViewById(R.id.filter_audio_button)
        filterDocumentsButton = findViewById(R.id.filter_documents_button)
    }

    private fun setupClickListeners() {
        findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            finish()
        }

        findViewById<FloatingActionButton>(R.id.add_file_fab).setOnClickListener {
            checkPermissionsAndPickFiles()
        }

        // Filter button listeners
        filterAllButton.setOnClickListener {
            applyFilter(null)
        }

        filterPhotosButton.setOnClickListener {
            applyFilter(FileType.PHOTO)
        }

        filterVideosButton.setOnClickListener {
            applyFilter(FileType.VIDEO)
        }

        filterAudioButton.setOnClickListener {
            applyFilter(FileType.AUDIO)
        }

        filterDocumentsButton.setOnClickListener {
            applyFilter(FileType.DOCUMENTS)
        }
    }
    private fun setupRecyclerView() {
        vaultAdapter = VaultAdapter(
            vaults = emptyList(),
            onDeleteClick = { file ->
                deleteFile(file)
            },
            onDownloadClick = { file ->
                downloadFile(file)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = vaultAdapter
    }

    private fun loadVaultItems() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Please log in to view vault", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                vaultItems = withContext(Dispatchers.IO) {
                    vaultRepository.getRecentVaultItems(uid)
                }
                updateFilterCounts()
                applyFilter(currentFilter)
            } catch (e: Exception) {
                Log.e("SecureVaultViewAll", "Error loading vault items", e)
                Toast.makeText(this@SecureVaultViewAll, "Error loading vault items", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFilterCounts() {
        val allFiles = vaultItems.flatMap { it.files }
        val photoCount = allFiles.count { it.type == FileType.PHOTO }
        val videoCount = allFiles.count { it.type == FileType.VIDEO }
        val audioCount = allFiles.count { it.type == FileType.AUDIO }
        val docCount = allFiles.count { it.type == FileType.DOCUMENTS }
        val totalCount = allFiles.size

        filterAllButton.text = "All ($totalCount)"
        filterPhotosButton.text = "Photos ($photoCount)"
        filterVideosButton.text = "Videos ($videoCount)"
        filterAudioButton.text = "Audio ($audioCount)"
        filterDocumentsButton.text = "Documents ($docCount)"
    }

    private fun applyFilter(filter: FileType?) {
        currentFilter = filter

        val filteredVaults = if (filter == null) {
            vaultItems
        } else {
            vaultItems.map { vault ->
                vault.copy(files = vault.files.filter { it.type == filter })
            }.filter { it.files.isNotEmpty() }
        }

        // Update button appearances
        updateFilterButtonAppearance()

        // Update adapter
        vaultAdapter = VaultAdapter(
            vaults = filteredVaults,
            onDeleteClick = { file -> deleteFile(file) },
            onDownloadClick = { file -> downloadFile(file) }
        )
        recyclerView.adapter = vaultAdapter
    }

    private fun updateFilterButtonAppearance() {
        // Reset all buttons to default appearance
        val buttons = listOf(filterAllButton, filterPhotosButton, filterVideosButton, filterAudioButton, filterDocumentsButton)
        buttons.forEach { button ->
            button.setBackgroundResource(R.drawable.filter_button_background)
            button.setTextColor(resources.getColor(R.color.filter_text_color, theme))
        }

        // Highlight selected button
        val selectedButton = when (currentFilter) {
            null -> filterAllButton
            FileType.PHOTO -> filterPhotosButton
            FileType.VIDEO -> filterVideosButton
            FileType.AUDIO -> filterAudioButton
            FileType.DOCUMENTS -> filterDocumentsButton
        }

        selectedButton.setBackgroundResource(R.drawable.filter_button_selected_background)
        selectedButton.setTextColor(resources.getColor(android.R.color.white, theme))
    }

    private fun deleteFile(file: VaultFile) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) {
                    vaultRepository.deleteFile(file)
                }
                Toast.makeText(this@SecureVaultViewAll, "File deleted successfully", Toast.LENGTH_SHORT).show()
                loadVaultItems() // Refresh data
            } catch (e: Exception) {
                Log.e("SecureVaultViewAll", "Error deleting file", e)
                Toast.makeText(this@SecureVaultViewAll, "Error deleting file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun downloadFile(file: VaultFile) {
        vaultRepository.getDownloadUrl(file) { url ->
            runOnUiThread {
                if (url != null) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Cannot open file", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Cannot get download URL", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // File upload functionality
    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val granted = perms.entries.all { it.value }
            if (granted) openFilePicker()
            else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }

    private val pickFilesLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (!uris.isNullOrEmpty()) {
                uploadSelectedFiles(uris)
            }
        }

    private fun checkPermissionsAndPickFiles() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ granular permissions
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            // Optional: Request all files access for documents
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {
            // Android 10 and below
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        Log.d("Permissions", "Requesting permissions: $permissions")
        requestStoragePermission.launch(permissions.toTypedArray())
    }

    private fun openFilePicker() {
        pickFilesLauncher.launch("*/*")
    }

    private fun uploadSelectedFiles(uris: List<Uri>) {
        val photoUris = mutableListOf<Uri>()
        val videoUris = mutableListOf<Uri>()
        val audioUris = mutableListOf<Uri>()
        val documentUris = mutableListOf<Uri>()

        uris.forEach { uri ->
            val mimeType = contentResolver.getType(uri)
            Log.d("FileType", "URI: $uri, MIME: $mimeType")

            when {
                mimeType?.startsWith("image/") == true -> photoUris.add(uri)
                mimeType?.startsWith("video/") == true -> videoUris.add(uri)
                mimeType?.startsWith("audio/") == true -> audioUris.add(uri)
                mimeType?.startsWith("application/") == true -> documentUris.add(uri)
                mimeType?.startsWith("text/") == true -> documentUris.add(uri)
                mimeType == "application/pdf" -> documentUris.add(uri)
                mimeType == "application/msword" -> documentUris.add(uri)
                mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> documentUris.add(uri)
                mimeType == "application/vnd.ms-excel" -> documentUris.add(uri)
                mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> documentUris.add(uri)
                else -> {
                    Log.w("FileType", "Unknown MIME type: $mimeType, treating as document")
                    documentUris.add(uri) // Default unknown files to documents
                }
            }
        }

        Log.d("Upload", "Categorized - Photos: ${photoUris.size}, Videos: ${videoUris.size}, Audio: ${audioUris.size}, Documents: ${documentUris.size}")

        Toast.makeText(this, "Uploading ${uris.size} files...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    vaultRepository.uploadVault(
                        photos = photoUris,
                        videos = videoUris,
                        audios = audioUris,
                        documents = documentUris
                    )
                }

                result.fold(
                    onSuccess = { vault ->
                        Toast.makeText(this@SecureVaultViewAll, "Upload successful!", Toast.LENGTH_SHORT).show()
                        loadVaultItems() // Refresh data
                    },
                    onFailure = { exception ->
                        Toast.makeText(this@SecureVaultViewAll, "Upload failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                        Log.e("Upload", "Upload failed", exception)
                    }
                )
            } catch (e: Exception) {
                Log.e("Upload", "Error uploading files", e)
                Toast.makeText(this@SecureVaultViewAll, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

