package com.iiest10356476.sheguard.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.iiest10356476.sheguard.data.models.FileType
import com.iiest10356476.sheguard.data.models.Vault
import com.iiest10356476.sheguard.data.models.VaultFile
import java.util.UUID

class VaultViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val vaultCollection = firestore.collection("Vault")
    private val TAG = "VaultViewModel"

    fun uploadFiles(files: List<Uri>) {
        val currentUser = auth.currentUser ?: return

        val vaultId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val vaultFiles = files.map { uri ->
            val type = when {
                uri.toString().contains("image") -> FileType.PHOTO
                uri.toString().contains("video") -> FileType.VIDEO
                uri.toString().contains("audio") -> FileType.AUDIO
                else -> FileType.PHOTO
            }
            VaultFile(url = uri.toString(), type = type)
        }

        val vaultItem = Vault(
            vaultId = vaultId,
            files = vaultFiles,
            submitDate = timestamp,
            uid = currentUser.uid
        )

        vaultCollection.document(vaultId)
            .set(vaultItem)
            .addOnSuccessListener { Log.d(TAG, "Vault item uploaded!") }
            .addOnFailureListener { e -> Log.e(TAG, "Upload failed", e) }
    }
}
