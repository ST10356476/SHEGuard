package com.iiest10356476.sheguard.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import com.google.firebase.firestore.Query

import com.google.firebase.storage.FirebaseStorage
import com.iiest10356476.sheguard.data.models.FileType
import com.iiest10356476.sheguard.data.models.Vault
import com.iiest10356476.sheguard.data.models.VaultFile
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

class VaultRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val vaultCollection = firestore.collection("Vault")
    private val TAG = "VaultRepository"


    // Save individual vault file
    suspend fun saveVaultFile(fileUrl: String, fileType: FileType): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("No user signed in"))

            val vaultFile = VaultFile(fileUrl, fileType)

            // Check if user already has a vault
            val snapshot = vaultCollection.whereEqualTo("uid", currentUser.uid).get().await()
            val existingVault = snapshot.documents.firstOrNull()?.toObject(Vault::class.java)

            if (existingVault != null) {
                // Append the new file to existing Vault
                val updatedFiles = existingVault.files + vaultFile
                vaultCollection.document(existingVault.vaultId).update("files", updatedFiles).await()

                Log.d(TAG, "Added file to existing vault: $fileUrl")

            } else {
                // Create a new Vault document
                val vaultId = UUID.randomUUID().toString()
                val newVault = Vault(
                    vaultId = vaultId,
                    files = listOf(vaultFile),
                    submitDate = System.currentTimeMillis(),
                    uid = currentUser.uid
                )
                vaultCollection.document(vaultId).set(newVault).await()

                Log.d(TAG, "Created new vault with file: $fileUrl")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving vault file", e)
            Result.failure(e)
        }
    }

    // Upload vault with multiple files
    suspend fun uploadVault(
        photos: List<Uri> = emptyList(),
        videos: List<Uri> = emptyList(),
        audios: List<Uri> = emptyList(),
        documents: List<Uri> = emptyList()
    ): Result<Vault> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("No user signed in"))

            if (photos.isEmpty() && videos.isEmpty() && audios.isEmpty() && documents.isEmpty()) {
                return Result.failure(Exception("No files to upload"))
            }

            val vaultId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()


            // Upload files and create VaultFile objects
            val photoFiles = uploadFiles("photos", photos, currentUser.uid, vaultId)
                .map { VaultFile(it, FileType.PHOTO) }
            val videoFiles = uploadFiles("videos", videos, currentUser.uid, vaultId)
                .map { VaultFile(it, FileType.VIDEO) }
            val audioFiles = uploadFiles("audios", audios, currentUser.uid, vaultId)
                .map { VaultFile(it, FileType.AUDIO) }
            val documentsFiles = uploadFiles("documents", documents, currentUser.uid, vaultId)
                .map { VaultFile(it, FileType.DOCUMENTS) }

            val allFiles = photoFiles + videoFiles + audioFiles + documentsFiles

            if (allFiles.isEmpty()) {
                return Result.failure(Exception("Failed to upload any files"))
            }

            // Check if user already has a vault
            val snapshot = vaultCollection.whereEqualTo("uid", currentUser.uid).get().await()
            val existingVault = snapshot.documents.firstOrNull()?.toObject(Vault::class.java)

            val resultVault = if (existingVault != null) {
                // Append files to existing vault
                val updatedFiles = existingVault.files + allFiles
                val updatedVault = existingVault.copy(files = updatedFiles as List<VaultFile>, submitDate = timestamp)
                vaultCollection.document(existingVault.vaultId).set(updatedVault).await()
                Log.d(TAG, "Added ${allFiles.size} files to existing vault")
                updatedVault
            } else {
                // Create new vault
                val newVault = Vault(vaultId, allFiles as List<VaultFile>, timestamp, currentUser.uid)
                vaultCollection.document(vaultId).set(newVault).await()
                Log.d(TAG, "Created new vault with ${allFiles.size} files")
                newVault
            }

            Result.success(resultVault)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading vault", e)
            Result.failure(e)
        }
    }

    // Safe delete file
    suspend fun deleteFile(file: VaultFile): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("No user signed in"))

            // Validate URL
            if (!file.url.startsWith("https://")) {
                Log.w(TAG, "Skipping delete, invalid storage URL: ${file.url}")
                return Result.failure(Exception("Invalid file URL"))
            }

            // Delete from Storage
            try {
                val ref = storage.getReferenceFromUrl(file.url)
                ref.delete().await()
                Log.d(TAG, "Deleted file from storage: ${file.url}")
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete file from storage (may not exist): ${file.url}", e)
                // Continue with Firestore deletion even if storage deletion fails
            }

            // Delete from Firestore
            val snapshot = vaultCollection.whereEqualTo("uid", currentUser.uid).get().await()
            var fileDeleted = false

            for (doc in snapshot.documents) {
                val vault = doc.toObject(Vault::class.java) ?: continue
                val updatedFiles = vault.files.filter { it.url != file.url }

                if (updatedFiles.size != vault.files.size) {
                    fileDeleted = true
                    if (updatedFiles.isEmpty()) {
                        // Delete entire vault if no files left
                        vaultCollection.document(vault.vaultId).delete().await()
                        Log.d(TAG, "Deleted empty vault: ${vault.vaultId}")
                    } else {
                        // Update vault with remaining files
                        vaultCollection.document(vault.vaultId).update("files", updatedFiles).await()
                        Log.d(TAG, "Updated vault with ${updatedFiles.size} remaining files")
                    }
                }
            }

            if (!fileDeleted) {
                return Result.failure(Exception("File not found in vault"))
            }

            Log.d(TAG, "Successfully deleted file from vault")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file", e)
            Result.failure(e)
        }
    }

    // Safe get download URL with callback
    fun getDownloadUrl(file: VaultFile, onComplete: (String?) -> Unit) {
        if (!file.url.startsWith("https://")) {
            Log.w(TAG, "Cannot download, invalid storage URL: ${file.url}")
            onComplete(null)
            return
        }

        try {
            val ref = storage.getReferenceFromUrl(file.url)
            ref.downloadUrl
                .addOnSuccessListener { uri -> onComplete(uri.toString()) }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to get download URL for ${file.url}", e)
                    onComplete(null)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting download URL", e)
            onComplete(null)
        }
    }


    // Get download URL with coroutines
    suspend fun getDownloadUrlAsync(file: VaultFile): Result<String> {
        return try {
            if (!file.url.startsWith("https://")) {
                return Result.failure(Exception("Invalid storage URL: ${file.url}"))
            }

            val ref = storage.getReferenceFromUrl(file.url)
            val uri = ref.downloadUrl.await()
            Result.success(uri.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error getting download URL", e)
            Result.failure(e)
        }
    }

    // Get current user's vault items with better sorting
    suspend fun getRecentVaultItems(uid: String): List<Vault> {
        return try {
            val snapshot = vaultCollection
                .whereEqualTo("uid", uid)
                .orderBy("submitDate", Query.Direction.DESCENDING)
                .get()
                .await()

            val items = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Vault::class.java)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse vault document: ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Retrieved ${items.size} vault items for user: $uid")
            items

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching recent vault items", e)
            emptyList()
        }
    }


    // Get vault statistics
    suspend fun getVaultStatistics(uid: String): VaultStatistics {
        return try {
            val vaults = getRecentVaultItems(uid)
            val allFiles = vaults.flatMap { it.files }

            VaultStatistics(
                totalFiles = allFiles.size,
                photoCount = allFiles.count { it.type == FileType.PHOTO },
                videoCount = allFiles.count { it.type == FileType.VIDEO },
                audioCount = allFiles.count { it.type == FileType.AUDIO },
                documentCount = allFiles.count { it.type == FileType.DOCUMENTS },
                totalVaults = vaults.size,
                lastUpload = vaults.maxOfOrNull { it.submitDate } ?: 0L
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting vault statistics", e)
            VaultStatistics()
        }
    }

    // Upload multiple files to Storage with better error handling
    private suspend fun uploadFiles(type: String, uris: List<Uri>, uid: String, vaultId: String): List<String> {
        if (uris.isEmpty()) return emptyList()

        val urls = mutableListOf<String>()

        for ((index, uri) in uris.withIndex()) {
            try {
                val fileName = "${System.currentTimeMillis()}_${index}_${UUID.randomUUID()}"
                val ref = storage.reference.child("vault/$uid/$vaultId/$type/$fileName")

                // Handle different URI schemes
                val safeUri = when {
                    uri.scheme.isNullOrBlank() -> Uri.fromFile(File(uri.path!!))
                    uri.scheme == "file" -> Uri.fromFile(File(uri.path!!))
                    else -> uri
                }

                // Upload file
                val uploadTask = ref.putFile(safeUri).await()
                val downloadUrl = ref.downloadUrl.await().toString()
                urls.add(downloadUrl)

                Log.d(TAG, "Successfully uploaded $type file ${index + 1}/${uris.size}: $downloadUrl")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload $type file ${index + 1}/${uris.size}: $uri", e)
                // Continue with other files even if one fails
            }
        }

        Log.d(TAG, "Upload summary for $type: ${urls.size}/${uris.size} files successful")

        return urls
    }
}

// Data class for vault statistics
data class VaultStatistics(
    val totalFiles: Int = 0,
    val photoCount: Int = 0,
    val videoCount: Int = 0,
    val audioCount: Int = 0,
    val documentCount: Int = 0,
    val totalVaults: Int = 0,
    val lastUpload: Long = 0L
)