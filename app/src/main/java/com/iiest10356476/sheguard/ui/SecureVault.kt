package com.iiest10356476.sheguard.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.data.models.FileType
import com.iiest10356476.sheguard.data.models.Vault
import com.iiest10356476.sheguard.data.repository.VaultRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class SecureVault : AppCompatActivity() {

    companion object {
        private const val TAG = "SecureVault"
    }

    private val vaultRepo = VaultRepository()
    private val selectedUris = mutableListOf<Uri>()

    // UI Components
    private lateinit var recentFilesRecyclerView: RecyclerView
    private lateinit var recentFilesAdapter: RecentFilesAdapter

    // File count TextViews
    private lateinit var totalFilesCount: TextView
    private lateinit var photosCount: TextView
    private lateinit var videosCount: TextView
    private lateinit var documentsCount: TextView
    private lateinit var othersCount: TextView
    private lateinit var photosFilesCount: TextView
    private lateinit var videosFilesCount: TextView
    private lateinit var audioFilesCount: TextView
    private lateinit var documentsFilesCount: TextView

    private var vaultItems: List<Vault> = emptyList()

    // Permission launcher
    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                Log.d(TAG, "Storage permissions granted")
                openFilePicker()
            } else {
                Log.w(TAG, "Storage permissions denied")
                Toast.makeText(this, "Storage permission is required to access files", Toast.LENGTH_LONG).show()
            }
        }

    // File picker launcher for all files
    private val pickFilesLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            handleSelectedFiles(uris, "general")
        }

    // File picker launcher for specific file types
    private val pickSpecificFilesLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            handleSelectedFiles(uris, "specific")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_secure_vault)

        // Set up back button
        val backButton = findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }

        setupWindowInsets()
        initializeViews()
        setupClickListeners()
        setupRecyclerView()
        loadVaultData()
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
            recentFilesRecyclerView = findViewById(R.id.recent_files_recycler_view)

            // File count TextViews
            totalFilesCount = findViewById(R.id.total_files_count)
            photosCount = findViewById(R.id.photos_count)
            videosCount = findViewById(R.id.videos_count)
            othersCount = findViewById(R.id.others_count)
            photosFilesCount = findViewById(R.id.photos_files_count)
            videosFilesCount = findViewById(R.id.videos_files_count)
            audioFilesCount = findViewById(R.id.audio_files_count)
            documentsFilesCount = findViewById(R.id.documents_files_count)

            Log.d(TAG, "All views initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
            Toast.makeText(this, "Error initializing interface", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        // Main action buttons - NO MORE AUTHENTICATION NEEDED
        findViewById<Button>(R.id.view_all_button).setOnClickListener {
            // Navigate directly to SecureVaultViewAll since user is already authenticated
            val intent = Intent(this, SecureVaultViewAll::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.upload_button).setOnClickListener {
            checkPermissionsAndPickFiles()
        }

        // Debug functions (for development)
        findViewById<Button>(R.id.upload_button).setOnLongClickListener {
            debugFirebaseData()
            true
        }

        findViewById<Button>(R.id.view_all_button).setOnLongClickListener {
            createTestData()
            true
        }

        // File type specific buttons
        setupFileTypeButtons()
    }

    private fun setupFileTypeButtons() {
        findViewById<LinearLayout>(R.id.add_photos_button).setOnClickListener {
            Log.d(TAG, "Photos button clicked")
            pickSpecificFileType("image/*")
        }

        findViewById<LinearLayout>(R.id.add_videos_button).setOnClickListener {
            Log.d(TAG, "Videos button clicked")
            pickSpecificFileType("video/*")
        }

        findViewById<LinearLayout>(R.id.add_audio_button).setOnClickListener {
            Log.d(TAG, "Audio button clicked")
            pickSpecificFileType("audio/*")
        }

        findViewById<LinearLayout>(R.id.add_documents_button).setOnClickListener {
            Log.d(TAG, "Documents button clicked")
            pickSpecificFileType("application/*,text/*")
        }
    }

    private fun setupRecyclerView() {
        recentFilesAdapter = RecentFilesAdapter(
            onDeleteClick = { file ->
                Log.d(TAG, "Delete clicked for file: ${file.url}")
                deleteFile(file)
            },
            onDownloadClick = { file ->
                Log.d(TAG, "Download clicked for file: ${file.url}")
                downloadFile(file)
            }
        )

        recentFilesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SecureVault, LinearLayoutManager.HORIZONTAL, false)
            adapter = recentFilesAdapter
        }

        Log.d(TAG, "RecyclerView setup completed")
    }

    private fun loadVaultData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Please log in to view vault", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "No authenticated user found")
            return
        }

        Log.d(TAG, "Loading vault data for user: $uid")

        lifecycleScope.launch {
            try {
                vaultItems = withContext(Dispatchers.IO) {
                    vaultRepo.getRecentVaultItems(uid)
                }

                Log.d(TAG, "Loaded ${vaultItems.size} vault items")
                updateUI()
                updateRecentFiles()

            } catch (e: Exception) {
                Log.e(TAG, "Error loading vault data", e)
                Toast.makeText(this@SecureVault, "Failed to load vault data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI() {
        val allFiles = vaultItems.flatMap { it.files }
        val filesByType = categorizeFiles(allFiles)

        Log.d(TAG, "File counts - Total: ${allFiles.size}, " +
                "Photos: ${filesByType.photos.size}, " +
                "Videos: ${filesByType.videos.size}, " +
                "Audio: ${filesByType.audio.size}, " +
                "Documents: ${filesByType.documents.size}")

        // Update UI counters
        with(filesByType) {
            totalFilesCount.text = allFiles.size.toString()
            photosCount.text = photos.size.toString()
            videosCount.text = videos.size.toString()
            othersCount.text = (audio.size + documents.size + others.size).toString()

            photosFilesCount.text = "${photos.size} files"
            videosFilesCount.text = "${videos.size} files"
            audioFilesCount.text = "${audio.size} files"
            documentsFilesCount.text = "${documents.size} files"
        }

        Log.d(TAG, "UI counters updated successfully")
    }

    private fun categorizeFiles(files: List<com.iiest10356476.sheguard.data.models.VaultFile>): CategorizedFiles {
        val photos = files.filter { it.type == FileType.PHOTO }
        val videos = files.filter { it.type == FileType.VIDEO }
        val audio = files.filter { it.type == FileType.AUDIO }
        val documents = files.filter { it.type == FileType.DOCUMENTS }
        val others = files - photos - videos - audio - documents

        return CategorizedFiles(photos, videos, audio, documents, others)
    }

    private data class CategorizedFiles(
        val photos: List<com.iiest10356476.sheguard.data.models.VaultFile>,
        val videos: List<com.iiest10356476.sheguard.data.models.VaultFile>,
        val audio: List<com.iiest10356476.sheguard.data.models.VaultFile>,
        val documents: List<com.iiest10356476.sheguard.data.models.VaultFile>,
        val others: List<com.iiest10356476.sheguard.data.models.VaultFile>
    )

    private fun updateRecentFiles() {
        val recentFiles = vaultItems
            .sortedByDescending { it.submitDate }
            .take(5)
            .flatMap { vault ->
                vault.files.map { file ->
                    RecentFileItem(file, vault.submitDate)
                }
            }
            .take(10)

        Log.d(TAG, "Recent files count: ${recentFiles.size}")
        recentFilesAdapter.updateFiles(recentFiles)
    }

    private fun handleSelectedFiles(uris: List<Uri>?, type: String) {
        if (uris.isNullOrEmpty()) {
            Log.d(TAG, "No files selected")
            return
        }

        Log.d(TAG, "Selected ${uris.size} $type files for upload")

        when (type) {
            "general" -> {
                selectedUris.clear()
                selectedUris.addAll(uris)
                uploadSelectedFiles()
            }
            "specific" -> uploadSpecificFiles(uris)
        }
    }

    private fun checkPermissionsAndPickFiles() {
        val permissions = getRequiredPermissions()
        Log.d(TAG, "Requesting permissions: $permissions")
        requestStoragePermission.launch(permissions.toTypedArray())
    }

    private fun getRequiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun openFilePicker() {
        Log.d(TAG, "Opening general file picker")
        pickFilesLauncher.launch("*/*")
    }

    private fun pickSpecificFileType(mimeType: String) {
        Log.d(TAG, "Opening specific file picker for: $mimeType")
        pickSpecificFilesLauncher.launch(mimeType)
    }

    private fun uploadSelectedFiles() {
        if (selectedUris.isEmpty()) {
            Log.w(TAG, "No files to upload")
            return
        }

        val categorizedUris = categorizeUrisByMimeType(selectedUris)
        Log.d(TAG, "Categorized files - Photos: ${categorizedUris.photos.size}, " +
                "Videos: ${categorizedUris.videos.size}, Audio: ${categorizedUris.audio.size}, " +
                "Documents: ${categorizedUris.documents.size}")

        uploadFiles(categorizedUris)
    }

    private fun uploadSpecificFiles(uris: List<Uri>) {
        val categorizedUris = categorizeUrisByMimeType(uris)
        Log.d(TAG, "Specific files categorized - Photos: ${categorizedUris.photos.size}, " +
                "Videos: ${categorizedUris.videos.size}, Audio: ${categorizedUris.audio.size}, " +
                "Documents: ${categorizedUris.documents.size}")

        uploadFiles(categorizedUris)
    }

    private fun categorizeUrisByMimeType(uris: List<Uri>): CategorizedUris {
        val photos = mutableListOf<Uri>()
        val videos = mutableListOf<Uri>()
        val audio = mutableListOf<Uri>()
        val documents = mutableListOf<Uri>()

        uris.forEach { uri ->
            val mimeType = contentResolver.getType(uri)
            Log.d(TAG, "File URI: $uri, MIME type: $mimeType")

            when {
                mimeType?.startsWith("image/") == true -> photos.add(uri)
                mimeType?.startsWith("video/") == true -> videos.add(uri)
                mimeType?.startsWith("audio/") == true -> audio.add(uri)
                mimeType?.startsWith("application/") == true ||
                        mimeType?.startsWith("text/") == true -> documents.add(uri)
                else -> documents.add(uri) // Default to documents for unknown files
            }
        }

        return CategorizedUris(photos, videos, audio, documents)
    }

    private data class CategorizedUris(
        val photos: List<Uri>,
        val videos: List<Uri>,
        val audio: List<Uri>,
        val documents: List<Uri>
    )

    private fun uploadFiles(categorizedUris: CategorizedUris) {
        val totalFiles = with(categorizedUris) {
            photos.size + videos.size + audio.size + documents.size
        }

        if (totalFiles == 0) {
            Toast.makeText(this, "No files selected", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "No files to upload after categorization")
            return
        }

        Toast.makeText(this, "Uploading $totalFiles files...", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Starting upload of $totalFiles files")

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    vaultRepo.uploadVault(
                        photos = categorizedUris.photos,
                        videos = categorizedUris.videos,
                        audios = categorizedUris.audio,
                        documents = categorizedUris.documents
                    )
                }

                // Handle upload result
                result.fold(
                    onSuccess = { vault ->
                        Toast.makeText(this@SecureVault, "Upload successful!", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Upload completed successfully: ${vault.vaultId}")
                        loadVaultData() // Refresh data
                    },
                    onFailure = { exception ->
                        val errorMessage = exception.message ?: "Unknown error occurred"
                        Toast.makeText(this@SecureVault, "Upload failed: $errorMessage", Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Upload failed", exception)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading files", e)
                Toast.makeText(this@SecureVault, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun deleteFile(file: com.iiest10356476.sheguard.data.models.VaultFile) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    vaultRepo.deleteFile(file)
                }
                Toast.makeText(this@SecureVault, "File deleted successfully", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "File deleted successfully: ${file.url}")
                loadVaultData() // Refresh data
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting file", e)
                Toast.makeText(this@SecureVault, "Failed to delete file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun downloadFile(file: com.iiest10356476.sheguard.data.models.VaultFile) {
        Log.d(TAG, "Attempting to download file: ${file.url}")

        vaultRepo.getDownloadUrl(file) { url ->
            runOnUiThread {
                if (url != null) {
                    Log.d(TAG, "Got download URL: $url")
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this@SecureVault, "Cannot open file", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Cannot open file", e)
                    }
                } else {
                    Toast.makeText(this@SecureVault, "Cannot get download URL", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Cannot get download URL for file: ${file.url}")
                }
            }
        }
    }

    // Debug function for development
    private fun debugFirebaseData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "No user authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Debugging Firebase data...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val snapshot = withContext(Dispatchers.IO) {
                    firestore.collection("Vault")
                        .whereEqualTo("uid", uid)
                        .get()
                        .await()
                }

                Log.d(TAG, "Debug: Firestore returned ${snapshot.documents.size} documents")

                val vaultItems = withContext(Dispatchers.IO) {
                    vaultRepo.getRecentVaultItems(uid)
                }

                Toast.makeText(this@SecureVault,
                    "Found ${snapshot.documents.size} docs, ${vaultItems.size} parsed vaults",
                    Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Log.e(TAG, "Debug error", e)
                Toast.makeText(this@SecureVault, "Debug failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Test function for development
    private fun createTestData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "No authenticated user", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Creating test data...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val testVault = hashMapOf(
                    "vaultId" to "test-${System.currentTimeMillis()}",
                    "uid" to currentUser.uid,
                    "submitDate" to System.currentTimeMillis(),
                    "files" to listOf(
                        hashMapOf(
                            "url" to "https://test-url.com/test.jpg",
                            "type" to "PHOTO"
                        )
                    )
                )

                withContext(Dispatchers.IO) {
                    firestore.collection("Vault")
                        .document(testVault["vaultId"] as String)
                        .set(testVault)
                        .await()
                }

                Toast.makeText(this@SecureVault, "Test data created!", Toast.LENGTH_SHORT).show()
                kotlinx.coroutines.delay(1000)
                loadVaultData()

            } catch (e: Exception) {
                Log.e(TAG, "Error creating test data", e)
                Toast.makeText(this@SecureVault, "Test failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Activity resumed, refreshing data")
        loadVaultData()
    }
}