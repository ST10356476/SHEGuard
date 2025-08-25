package com.iiest10356476.sheguard.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.data.models.FileType
import com.iiest10356476.sheguard.data.models.Vault
import com.iiest10356476.sheguard.data.repository.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SecureVault : AppCompatActivity() {

    private val vaultRepo = VaultRepository() // Using your current constructor
    private val selectedUris = mutableListOf<Uri>()
    private lateinit var recentFilesRecyclerView: RecyclerView
    private lateinit var recentFilesAdapter: RecentFilesAdapter

    // UI Elements for file counts
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_secure_vault)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupClickListeners()
        setupRecyclerView()
        loadVaultData()
    }

    private fun initializeViews() {
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

        Log.d("SecureVault", "All views initialized successfully")
    }

    private fun setupClickListeners() {
        findViewById<Button>(R.id.view_all_button).setOnClickListener {
            val intent = Intent(this, SecureVaultViewAll::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.upload_button).setOnClickListener {
            checkPermissionsAndPickFiles()
        }

        // ADD DEBUG FUNCTION - Long press upload button to debug
        findViewById<Button>(R.id.upload_button).setOnLongClickListener {
            debugFirebaseData()
            true
        }

        // ADD TEST FUNCTION - Long press view all button to create test data
        findViewById<Button>(R.id.view_all_button).setOnLongClickListener {
            testSimpleUpload()
            true
        }

        // Individual file type buttons
        findViewById<LinearLayout>(R.id.add_photos_button).setOnClickListener {
            Log.d("SecureVault", "Photos button clicked")
            pickSpecificFileType("image/*")
        }

        findViewById<LinearLayout>(R.id.add_videos_button).setOnClickListener {
            Log.d("SecureVault", "Videos button clicked")
            pickSpecificFileType("video/*")
        }

        findViewById<LinearLayout>(R.id.add_audio_button).setOnClickListener {
            Log.d("SecureVault", "Audio button clicked")
            pickSpecificFileType("audio/*")
        }

        findViewById<LinearLayout>(R.id.add_documents_button).setOnClickListener {
            Log.d("SecureVault", "Documents button clicked")
            // Multiple MIME types for documents
            pickSpecificFileType("application/*,text/*")  // This allows PDFs, Word docs, text files, etc.
        }
    }

    // DEBUG FUNCTION
    private fun debugFirebaseData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.e("DEBUG", "No authenticated user")
            Toast.makeText(this, "No user authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("DEBUG", "Current user: $uid")
        Toast.makeText(this, "Checking Firebase data...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Debug Firestore directly
                val firestore = FirebaseFirestore.getInstance()
                val snapshot = withContext(Dispatchers.IO) {
                    firestore.collection("Vault")
                        .whereEqualTo("uid", uid)
                        .get()
                        .await()
                }

                Log.d("DEBUG", "📊 Firestore query returned ${snapshot.documents.size} documents")

                if (snapshot.documents.isEmpty()) {
                    Log.w("DEBUG", "⚠️ No documents found for user $uid")
                    Toast.makeText(this@SecureVault, "No vault data found in Firestore", Toast.LENGTH_LONG).show()

                    // Check if any documents exist at all
                    val allDocs = withContext(Dispatchers.IO) {
                        firestore.collection("Vault").limit(5).get().await()
                    }
                    Log.d("DEBUG", "📋 Total documents in Vault collection: ${allDocs.documents.size}")

                    return@launch
                }

                // Parse each document
                snapshot.documents.forEachIndexed { index, document ->
                    Log.d("DEBUG", "📄 Document $index:")
                    Log.d("DEBUG", "  - ID: ${document.id}")
                    Log.d("DEBUG", "  - UID: ${document.getString("uid")}")
                    Log.d("DEBUG", "  - VaultID: ${document.getString("vaultId")}")
                    Log.d("DEBUG", "  - SubmitDate: ${document.getLong("submitDate")}")

                    val filesArray = document.get("files") as? List<*>
                    Log.d("DEBUG", "  - Files array size: ${filesArray?.size ?: 0}")

                    filesArray?.forEachIndexed { fileIndex, fileData ->
                        if (fileData is Map<*, *>) {
                            val url = fileData["url"] as? String
                            val type = fileData["type"] as? String
                            Log.d("DEBUG", "    File $fileIndex: URL=$url, Type=$type")
                        }
                    }
                }

                // Try to use VaultRepository
                val vaultItems = withContext(Dispatchers.IO) {
                    vaultRepo.getRecentVaultItems(uid)
                }

                Log.d("DEBUG", "🔄 VaultRepository returned ${vaultItems.size} items")
                vaultItems.forEachIndexed { index, vault ->
                    Log.d("DEBUG", "  Vault $index: ${vault.vaultId} with ${vault.files.size} files")
                }

                Toast.makeText(this@SecureVault,
                    "Found ${snapshot.documents.size} docs, ${vaultItems.size} parsed vaults",
                    Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Log.e("DEBUG", "Error debugging Firebase data", e)
                Toast.makeText(this@SecureVault, "Debug error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // TEST FUNCTION - Add this to create test data
    private fun testSimpleUpload() {
        Toast.makeText(this, "Testing simple upload...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    Toast.makeText(this@SecureVault, "No authenticated user", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Create a simple test vault directly
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

                Toast.makeText(this@SecureVault, "Test vault created! Now refresh...", Toast.LENGTH_SHORT).show()

                // Wait a moment then reload data
                kotlinx.coroutines.delay(1000)
                loadVaultData()

            } catch (e: Exception) {
                Log.e("DEBUG", "Error creating test vault", e)
                Toast.makeText(this@SecureVault, "Test failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupRecyclerView() {
        recentFilesAdapter = RecentFilesAdapter(
            onDeleteClick = { file ->
                Log.d("SecureVault", "Delete clicked for file: ${file.url}")
                deleteFile(file)
            },
            onDownloadClick = { file ->
                Log.d("SecureVault", "Download clicked for file: ${file.url}")
                downloadFile(file)
            }
        )
        recentFilesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recentFilesRecyclerView.adapter = recentFilesAdapter
        Log.d("SecureVault", "RecyclerView setup completed")
    }

    private fun loadVaultData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Please log in to view vault", Toast.LENGTH_SHORT).show()
            Log.e("SecureVault", "No authenticated user found")
            return
        }

        Log.d("SecureVault", "Loading vault data for user: $uid")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                vaultItems = withContext(Dispatchers.IO) {
                    vaultRepo.getRecentVaultItems(uid)
                }

                Log.d("SecureVault", "Loaded ${vaultItems.size} vault items")
                vaultItems.forEach { vault ->
                    Log.d("SecureVault", "Vault ${vault.vaultId} has ${vault.files.size} files")
                }

                updateUI()
                updateRecentFiles()

            } catch (e: Exception) {
                Log.e("SecureVault", "Error loading vault data", e)
                Toast.makeText(this@SecureVault, "Error loading vault data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI() {
        val allFiles = vaultItems.flatMap { it.files }
        val photoFiles = allFiles.filter { it.type == FileType.PHOTO }
        val videoFiles = allFiles.filter { it.type == FileType.VIDEO }
        val audioFiles = allFiles.filter { it.type == FileType.AUDIO }
        val documentFiles = allFiles.filter { it.type == FileType.DOCUMENTS }
        val otherFiles = allFiles - photoFiles - videoFiles - audioFiles - documentFiles

        Log.d("SecureVault", "File counts - Total: ${allFiles.size}, Photos: ${photoFiles.size}, Videos: ${videoFiles.size}, Audio: ${audioFiles.size}, Documents: ${documentFiles.size}, Others: ${otherFiles.size}")

        // Update counters
        totalFilesCount.text = allFiles.size.toString()
        photosCount.text = photoFiles.size.toString()
        videosCount.text = videoFiles.size.toString()
        othersCount.text = (audioFiles.size + documentFiles.size + otherFiles.size).toString()

        photosFilesCount.text = "${photoFiles.size} files"
        videosFilesCount.text = "${videoFiles.size} files"
        audioFilesCount.text = "${audioFiles.size} files"
        documentsFilesCount.text = "${documentFiles.size} files"

        Log.d("SecureVault", "UI counters updated successfully")
    }

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

        Log.d("SecureVault", "Recent files count: ${recentFiles.size}")
        recentFiles.forEach { item ->
            Log.d("SecureVault", "Recent file: ${item.file.type} - ${item.file.url}")
        }

        recentFilesAdapter.updateFiles(recentFiles)
    }

    private fun deleteFile(file: com.iiest10356476.sheguard.data.models.VaultFile) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) {
                    vaultRepo.deleteFile(file)
                }
                Toast.makeText(this@SecureVault, "File deleted successfully", Toast.LENGTH_SHORT).show()
                Log.d("SecureVault", "File deleted successfully: ${file.url}")
                loadVaultData() // Refresh data
            } catch (e: Exception) {
                Log.e("SecureVault", "Error deleting file", e)
                Toast.makeText(this@SecureVault, "Error deleting file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun downloadFile(file: com.iiest10356476.sheguard.data.models.VaultFile) {
        Log.d("SecureVault", "Attempting to download file: ${file.url}")
        vaultRepo.getDownloadUrl(file) { url ->
            runOnUiThread {
                if (url != null) {
                    Log.d("SecureVault", "Got download URL: $url")
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Cannot open file", Toast.LENGTH_SHORT).show()
                        Log.e("SecureVault", "Cannot open file", e)
                    }
                } else {
                    Toast.makeText(this, "Cannot get download URL", Toast.LENGTH_SHORT).show()
                    Log.e("SecureVault", "Cannot get download URL for file: ${file.url}")
                }
            }
        }
    }

    // Permission launcher
    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val granted = perms.entries.all { it.value }
            if (granted) {
                Log.d("SecureVault", "Storage permissions granted")
                openFilePicker()
            } else {
                Log.w("SecureVault", "Storage permissions denied")
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    // File picker launcher for all files
    private val pickFilesLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (!uris.isNullOrEmpty()) {
                Log.d("SecureVault", "Selected ${uris.size} files for upload")
                selectedUris.clear()
                selectedUris.addAll(uris)
                uploadSelectedFiles()
            } else {
                Log.d("SecureVault", "No files selected")
            }
        }

    // File picker launcher for specific file types
    private val pickSpecificFilesLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (!uris.isNullOrEmpty()) {
                Log.d("SecureVault", "Selected ${uris.size} specific files for upload")
                uploadSpecificFiles(uris)
            } else {
                Log.d("SecureVault", "No specific files selected")
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
        Log.d("SecureVault", "Requesting permissions: $permissions")
        requestStoragePermission.launch(permissions.toTypedArray())
    }

    private fun openFilePicker() {
        Log.d("SecureVault", "Opening general file picker")
        pickFilesLauncher.launch("*/*")
    }

    private fun pickSpecificFileType(mimeType: String) {
        Log.d("SecureVault", "Opening specific file picker for: $mimeType")
        pickSpecificFilesLauncher.launch(mimeType)
    }

    private fun uploadSelectedFiles() {
        if (selectedUris.isEmpty()) {
            Log.w("SecureVault", "No files to upload")
            return
        }

        val photoUris = selectedUris.filter { uri ->
            contentResolver.getType(uri)?.startsWith("image/") == true
        }
        val videoUris = selectedUris.filter { uri ->
            contentResolver.getType(uri)?.startsWith("video/") == true
        }
        val audioUris = selectedUris.filter { uri ->
            contentResolver.getType(uri)?.startsWith("audio/") == true
        }

        val documentUris = selectedUris.filter { uri ->
            contentResolver.getType(uri)?.startsWith("documents/") == true
        }

        Log.d("SecureVault", "Categorized files - Photos: ${photoUris.size}, Videos: ${videoUris.size}, Audio: ${audioUris.size}")
        uploadFiles(photoUris, videoUris, audioUris, documentUris)
    }

    private fun uploadSpecificFiles(uris: List<Uri>) {
        val photoUris = mutableListOf<Uri>()
        val videoUris = mutableListOf<Uri>()
        val audioUris = mutableListOf<Uri>()
        val documentUris = mutableListOf<Uri>()

        uris.forEach { uri ->
            val mimeType = contentResolver.getType(uri)
            Log.d("SecureVault", "File URI: $uri, MIME type: $mimeType")
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
                else -> documentUris.add(uri) // Default to documents for unknown files
            }
        }

        Log.d("SecureVault", "Specific files categorized - Photos: ${photoUris.size}, Videos: ${videoUris.size}, Audio: ${audioUris.size}, Documents: ${documentUris.size}")
        uploadFiles(photoUris, videoUris, audioUris, documentUris)
    }

    private fun uploadFiles(photoUris: List<Uri>, videoUris: List<Uri>, audioUris: List<Uri>, documentUris: List<Uri>) {
        if (photoUris.isEmpty() && videoUris.isEmpty() && audioUris.isEmpty() && documentUris.isEmpty()) {
            Toast.makeText(this, "No files selected", Toast.LENGTH_SHORT).show()
            Log.w("SecureVault", "No files to upload after categorization")
            return
        }

        val totalFiles = photoUris.size + videoUris.size + audioUris.size + documentUris.size
        Toast.makeText(this, "Uploading $totalFiles files...", Toast.LENGTH_SHORT).show()
        Log.d("SecureVault", "Starting upload of $totalFiles files")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    vaultRepo.uploadVault(
                        photos = photoUris,
                        videos = videoUris,
                        audios = audioUris,
                        documents = documentUris
                    )
                }

                // Handle Result type properly
                result.fold(
                    onSuccess = { vault ->
                        Toast.makeText(this@SecureVault, "Upload successful!", Toast.LENGTH_SHORT).show()
                        Log.d("SecureVault", "Upload completed successfully")
                        loadVaultData() // Refresh data to show new files
                    },
                    onFailure = { exception ->
                        val errorMessage = exception.message ?: "Unknown error"
                        Toast.makeText(this@SecureVault, "Upload failed: $errorMessage", Toast.LENGTH_LONG).show()
                        Log.e("SecureVault", "Upload failed", exception)
                    }
                )
            } catch (e: Exception) {
                Log.e("SecureVault", "Error uploading files", e)
                Toast.makeText(this@SecureVault, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("SecureVault", "Activity resumed, refreshing data")
        loadVaultData() // Refresh data when returning to activity
    }
}