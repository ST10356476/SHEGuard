package com.iiest10356476.sheguard.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.iiest10356476.sheguard.data.models.Vault
import java.util.UUID

class VaultViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val vaultCollection = firestore.collection("Vault")
    private val TAG = "VaultViewModel"

    fun uploadFiles(files: List<Uri>) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "No user signed in. Cannot upload vault item.")
            return
        }

        val vaultId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val photos = files.map { it.toString() }
        val vaultItem = Vault(
            vaultId = vaultId,
            photos = photos,
            videos = emptyList(),
            audios = emptyList(),
            submitDate = timestamp,
            uid = currentUser.uid
        )

        vaultCollection.document(vaultId)
            .set(vaultItem)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Vault item uploaded successfully!")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error uploading vault item", e)
            }
    }
}
