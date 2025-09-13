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
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.data.models.FileType
import com.iiest10356476.sheguard.data.models.Vault
import com.iiest10356476.sheguard.data.models.VaultFile
import com.iiest10356476.sheguard.data.repository.VaultRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SecureVaultViewAll : AppCompatActivity() {

    companion object {
        private const val TAG = "SecureVaultViewAll"
    }

    // UI Components
    private lateinit var recyclerView: RecyclerView
    private lateinit var vaultAdapter: VaultAdapter

    // Filter buttons
    private lateinit var filterAllButton: Button
    private lateinit var filterPhotosButton: Button
    private lateinit var filterVideosButton: Button
    private lateinit var filterAudioButton: Button
    private lateinit var filterDocumentsButton: Button

    // Data and state
    private val vaultRepository = VaultRepository()
    private var vaultItems: List<Vault> = emptyList()
    private var currentFilter: FileType? = null



    // Permission and file picker launchers
    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                openFilePicker()
            } else {
                Toast.makeText(this, "Storage permission is required to access files", Toast.LENGTH_LONG).show()
            }
        }

    private val pickFilesLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (!uris.isNullOrEmpty()) {
                uploadSelectedFiles(uris)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_secure_vault_view_all)
        setupWindowInsets()
        initializeViews()
        setupClickListeners()
        setupRecyclerView()
        loadVaultItems()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeViews() {
        try {
            recyclerView = findViewById(R.id.files_recycler_view)

            // Filter buttons
            filterAllButton = findViewById(R.id.filter_all_button)
            filterPhotosButton = findViewById(R.id.filter_photos_button)
            filterVideosButton = findViewById(R.id.filter_videos_button)
            filterAudioButton = findViewById(R.id.filter_audio_button)
            filterDocumentsButton = findViewById(R.id.filter_documents_button)

            Log.d(TAG, "All views initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
            Toast.makeText(this, "Error initializing interface", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        // Navigation
        findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            finish()
        }

        findViewById<FloatingActionButton>(R.id.add_file_fab).setOnClickListener {
            checkPermissionsAndPickFiles()
        }

        // Filter buttons
        setupFilterButtonListeners()
    }

    private fun setupFilterButtonListeners() {
        filterAllButton.setOnClickListener { applyFilter(null) }
        filterPhotosButton.setOnClickListener { applyFilter(FileType.PHOTO) }
        filterVideosButton.setOnClickListener { applyFilter(FileType.VIDEO) }
        filterAudioButton.setOnClickListener { applyFilter(FileType.AUDIO) }
        filterDocumentsButton.setOnClickListener { applyFilter(FileType.DOCUMENTS) }
    }

    private fun setupRecyclerView() {
        vaultAdapter = VaultAdapter(
            vaults = emptyList(),
            onDeleteClick = { file -> deleteFile(file) },
            onDownloadClick = { file -> downloadFile(file) }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SecureVaultViewAll)
            adapter = vaultAdapter
        }

        Log.d(TAG, "RecyclerView setup completed")
    }

    private fun loadVaultItems() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Please log in to view vault", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "No authenticated user found")
            return
        }

        Log.d(TAG, "Loading vault items for user: $uid")

        lifecycleScope.launch {
            try {
                vaultItems = withContext(Dispatchers.IO) {
                    vaultRepository.getRecentVaultItems(uid)
                }

                Log.d(TAG, "Loaded ${vaultItems.size} vault items")
                updateFilterCounts()
                applyFilter(currentFilter)

            } catch (e: Exception) {
                Log.e(TAG, "Error loading vault items", e)
                Toast.makeText(this@SecureVaultViewAll, "Failed to load vault items", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFilterCounts() {
        val allFiles = vaultItems.flatMap { it.files }
        val fileCounts = FileTypeCounts(
            total = allFiles.size,
            photos = allFiles.count { it.type == FileType.PHOTO },
            videos = allFiles.count { it.type == FileType.VIDEO },
            audio = allFiles.count { it.type == FileType.AUDIO },
            documents = allFiles.count { it.type == FileType.DOCUMENTS }
        )

        // Update button texts with counts
        filterAllButton.text = "All (${fileCounts.total})"
        filterPhotosButton.text = "Photos (${fileCounts.photos})"
        filterVideosButton.text = "Videos (${fileCounts.videos})"
        filterAudioButton.text = "Audio (${fileCounts.audio})"
        filterDocumentsButton.text = "Documents (${fileCounts.documents})"

        Log.d(TAG, "Filter counts updated: $fileCounts")
    }

    private fun navigateWithAuthentication(targetActivity: String) {
        val intent = Intent(this, SecureActivity::class.java)
        intent.putExtra(SecureActivity.EXTRA_TARGET_ACTIVITY, targetActivity)
        startActivity(intent)
    }

    private data class FileTypeCounts(
        val total: Int,
        val photos: Int,
        val videos: Int,
        val audio: Int,
        val documents: Int
    )

    private fun applyFilter(filter: FileType?) {
        currentFilter = filter

        val filteredVaults = if (filter == null) {
            vaultItems
        } else {
            vaultItems.mapNotNull { vault ->
                val filteredFiles = vault.files.filter { it.type == filter }
                if (filteredFiles.isNotEmpty()) {
                    vault.copy(files = filteredFiles)
                } else null
            }
        }

        Log.d(TAG, "Applied filter: $filter, showing ${filteredVaults.size} vaults")

        updateFilterButtonAppearance()
        updateAdapter(filteredVaults)
    }

    private fun updateAdapter(vaults: List<Vault>) {
        vaultAdapter = VaultAdapter(
            vaults = vaults,
            onDeleteClick = { file -> deleteFile(file) },
            onDownloadClick = { file -> downloadFile(file) }
        )
        recyclerView.adapter = vaultAdapter
    }

    private fun updateFilterButtonAppearance() {
        val allButtons = listOf(
            filterAllButton, filterPhotosButton, filterVideosButton,
            filterAudioButton, filterDocumentsButton
        )

        // Reset all buttons to default appearance
        allButtons.forEach { button ->
            button.setBackgroundResource(R.drawable.filter_button_background)
            button.setTextColor(ContextCompat.getColor(this, R.color.filter_text_color))
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
        selectedButton.setTextColor(ContextCompat.getColor(this, android.R.color.white))
    }

    private fun deleteFile(file: VaultFile) {
        Log.d(TAG, "Deleting file: ${file.url}")

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    vaultRepository.deleteFile(file)
                }

                Toast.makeText(this@SecureVaultViewAll, "File deleted successfully", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "File deleted successfully: ${file.url}")
                loadVaultItems() // Refresh data

            } catch (e: Exception) {
                Log.e(TAG, "Error deleting file", e)
                Toast.makeText(this@SecureVaultViewAll, "Failed to delete file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun downloadFile(file: VaultFile) {
        Log.d(TAG, "Downloading file: ${file.url}")

        vaultRepository.getDownloadUrl(file) { url ->
            runOnUiThread {
                if (url != null) {
                    Log.d(TAG, "Got download URL: $url")
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this@SecureVaultViewAll, "Cannot open file", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Cannot open file", e)
                    }
                } else {
                    Toast.makeText(this@SecureVaultViewAll, "Cannot get download URL", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Cannot get download URL for file: ${file.url}")
                }
            }
        }
    }

    // File upload functionality
    private fun checkPermissionsAndPickFiles() {
        val permissions = getRequiredPermissions()
        Log.d(TAG, "Requesting permissions: $permissions")
        requestStoragePermission.launch(permissions.toTypedArray())
    }

    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ granular permissions
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11-12
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else -> {
                // Android 10 and below
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        return permissions
    }

    private fun openFilePicker() {
        Log.d(TAG, "Opening file picker")
        pickFilesLauncher.launch("*/*")
    }

    private fun uploadSelectedFiles(uris: List<Uri>) {
        val categorizedUris = categorizeUrisByType(uris)
        val totalFiles = categorizedUris.getTotalCount()

        if (totalFiles == 0) {
            Toast.makeText(this, "No files selected", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Uploading $totalFiles files: $categorizedUris")
        Toast.makeText(this, "Uploading $totalFiles files...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    vaultRepository.uploadVault(
                        photos = categorizedUris.photos,
                        videos = categorizedUris.videos,
                        audios = categorizedUris.audio,
                        documents = categorizedUris.documents
                    )
                }

                result.fold(
                    onSuccess = { vault ->
                        Toast.makeText(this@SecureVaultViewAll, "Upload successful!", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Upload completed successfully: ${vault.vaultId}")
                        loadVaultItems() // Refresh data
                    },
                    onFailure = { exception ->
                        val errorMessage = exception.message ?: "Unknown error occurred"
                        Toast.makeText(this@SecureVaultViewAll, "Upload failed: $errorMessage", Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Upload failed", exception)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading files", e)
                Toast.makeText(this@SecureVaultViewAll, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun categorizeUrisByType(uris: List<Uri>): CategorizedUris {
        val photos = mutableListOf<Uri>()
        val videos = mutableListOf<Uri>()
        val audio = mutableListOf<Uri>()
        val documents = mutableListOf<Uri>()

        uris.forEach { uri ->
            val mimeType = contentResolver.getType(uri)
            Log.d(TAG, "Categorizing URI: $uri, MIME: $mimeType")

            when {
                mimeType?.startsWith("image/") == true -> photos.add(uri)
                mimeType?.startsWith("video/") == true -> videos.add(uri)
                mimeType?.startsWith("audio/") == true -> audio.add(uri)
                isDocumentMimeType(mimeType) -> documents.add(uri)
                else -> {
                    Log.w(TAG, "Unknown MIME type: $mimeType, treating as document")
                    documents.add(uri) // Default unknown files to documents
                }
            }
        }

        return CategorizedUris(photos, videos, audio, documents)
    }

    private fun isDocumentMimeType(mimeType: String?): Boolean {
        return when (mimeType) {
            null -> false
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> true
            else -> mimeType.startsWith("application/") || mimeType.startsWith("text/")
        }
    }

    private data class CategorizedUris(
        val photos: List<Uri>,
        val videos: List<Uri>,
        val audio: List<Uri>,
        val documents: List<Uri>
    ) {
        fun getTotalCount(): Int = photos.size + videos.size + audio.size + documents.size

        override fun toString(): String {
            return "Photos: ${photos.size}, Videos: ${videos.size}, Audio: ${audio.size}, Documents: ${documents.size}"
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Activity resumed, refreshing data")
        loadVaultItems()
    }
}