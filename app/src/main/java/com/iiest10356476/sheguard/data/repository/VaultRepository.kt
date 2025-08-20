package com.iiest10356476.sheguard.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.iiest10356476.sheguard.data.models.Vault
import kotlinx.coroutines.tasks.await
import java.util.Collections.emptyList
import java.util.UUID
import kotlin.Result

class VaultRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val vaultCollection = firestore.collection("Vault")
    private val TAG = "VaultRepository"

    /**
     * Upload a new Vault entry for the current user
     * Just saves data to Firestore, like AuthRepository
     */
    suspend fun uploadVault(
        photos: List<String> = emptyList(),
        videos: List<String> = emptyList(),
        audios: List<String> = emptyList()
    ): Result<Vault> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("No user signed in"))

            val vaultId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            val vaultItem = Vault(
                vaultId = vaultId,
                photos = photos,
                videos = videos,
                audios = audios,
                submitDate = timestamp,
                uid = currentUser.uid
            )

            // Save to Firestore
            vaultCollection.document(vaultId).set(vaultItem).await()
            Log.d(TAG, "Vault item uploaded successfully!")

            Result.success(vaultItem)
        } catch (e: Exception) {
            Log.e(TAG, " Error uploading vault", e)
            Result.failure(e)
        }
    }
}
