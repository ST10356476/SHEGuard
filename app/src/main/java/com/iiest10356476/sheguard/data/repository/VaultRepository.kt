package com.iiest10356476.sheguard.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

    // Upload vault with photos/videos/audios

    // VaultRepository.kt
    suspend fun saveVaultFile(fileUrl: String, fileType: FileType) {
        try {
            val currentUser = auth.currentUser ?: return

            // Create a new VaultFile
            val vaultFile = VaultFile(fileUrl, fileType)

            // Check if user already has a vault
            val snapshot = vaultCollection.whereEqualTo("uid", currentUser.uid).get().await()
            val existingVault = snapshot.documents.firstOrNull()?.toObject(Vault::class.java)

            if (existingVault != null) {
                // Append the new file to existing Vault
                val updatedFiles = existingVault.files + vaultFile
                vaultCollection.document(existingVault.vaultId).update("files", updatedFiles).await()
                Log.d("VaultRepository", "Added file to existing vault: $fileUrl")
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
                Log.d("VaultRepository", "Created new vault with file: $fileUrl")
            }

        } catch (e: Exception) {
            Log.e("VaultRepository", "Error saving vault file", e)
        }
    }

    suspend fun uploadVault(
        photos: List<Uri> = emptyList(),
        videos: List<Uri> = emptyList(),
        audios: List<Uri> = emptyList()
    ): Result<Vault> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("No user signed in"))

            val vaultId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            val photoFiles = uploadFiles("photos", photos, currentUser.uid, vaultId).map { VaultFile(it, FileType.PHOTO) }
            val videoFiles = uploadFiles("videos", videos, currentUser.uid, vaultId).map { VaultFile(it, FileType.VIDEO) }
            val audioFiles = uploadFiles("audios", audios, currentUser.uid, vaultId).map { VaultFile(it, FileType.AUDIO) }

            val allFiles = photoFiles + videoFiles + audioFiles

            val vaultItem = Vault(vaultId, allFiles, timestamp, currentUser.uid)
            vaultCollection.document(vaultId).set(vaultItem).await()
            Log.d(TAG, "Vault item uploaded successfully!")

            Result.success(vaultItem)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading vault", e)
            Result.failure(e)
        }
    }

    // Safe delete file
    suspend fun deleteFile(file: VaultFile) {
        try {
            val currentUser = auth.currentUser ?: return

            // Skip placeholders
            if (!file.url.startsWith("https://")) {
                Log.w(TAG, "Skipping delete, invalid storage URL: ${file.url}")
                return
            }

            // Delete from Storage
            val ref = storage.getReferenceFromUrl(file.url)
            ref.delete().await()
            Log.d(TAG, "Deleted file from storage: ${file.url}")

            // Delete from Firestore
            val snapshot = vaultCollection.whereEqualTo("uid", currentUser.uid).get().await()
            for (doc in snapshot.documents) {
                val vault = doc.toObject(Vault::class.java) ?: continue
                val updatedFiles = vault.files.filter { it.url != file.url }
                vaultCollection.document(vault.vaultId).update("files", updatedFiles).await()
            }
            Log.d(TAG, "Deleted file from Firestore successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file", e)
        }
    }

    // Safe get download URL
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

    // Get current user's vault items
    suspend fun getRecentVaultItems(uid: String): List<Vault> {
        return try {
            val snapshot = vaultCollection.whereEqualTo("uid", uid).get().await()
            val items = snapshot.documents.mapNotNull { it.toObject(Vault::class.java) }
            items.sortedByDescending { it.submitDate }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching recent vault items", e)
            emptyList()
        }
    }

    // Upload multiple files to Storage
    private suspend fun uploadFiles(type: String, uris: List<Uri>, uid: String, vaultId: String): List<String> {
        val urls = mutableListOf<String>()
        for (uri in uris) {
            try {
                val fileName = UUID.randomUUID().toString()
                val ref = storage.reference.child("vault/$uid/$vaultId/$type/$fileName")

                val safeUri = if (uri.scheme.isNullOrBlank() || uri.scheme == "file") {
                    Uri.fromFile(File(uri.path!!))
                } else uri

                ref.putFile(safeUri).await()
                val downloadUrl = ref.downloadUrl.await().toString()
                urls.add(downloadUrl)
                Log.d(TAG, "Uploaded $type file URL: $downloadUrl")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload a $type file: $uri", e)
            }
        }
        return urls
    }
}
