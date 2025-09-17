package com.iiest10356476.sheguard.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class SettingsRepo {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun deleteCurrentUserData(onComplete: (Boolean, String?) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onComplete(false, "No logged-in user found")
            return
        }

        val uid = currentUser.uid

        // Delete all Firestore + Storage
        deleteAllUserData(uid) { success, error ->
            if (success) {
                // Try to delete FirebaseAuth account
                currentUser.delete()
                    .addOnSuccessListener { onComplete(true, null) }
                    .addOnFailureListener { e ->
                        // Could fail if recent login required
                        onComplete(false, "Data deleted but account not removed: ${e.message}")
                    }
            } else {
                onComplete(false, error)
            }
        }
    }

    private fun deleteAllUserData(uid: String, onComplete: (Boolean, String?) -> Unit) {
        val db = firestore
        val storage = storage

        val userRef = db.collection("users").document(uid)

        userRef.delete().addOnSuccessListener {
            deleteCollection(db, "emergencyContacts/$uid/contacts")
            deleteCollection(db, "panicEvents/$uid/events")
            deleteCollection(db, "recentFiles/$uid/files")
            deleteCollection(db, "vaults/$uid/vault")

            deleteDocsByUid(db, "Vault", uid)
            deleteDocsByUid(db, "EmergencyContact", uid)
            deleteDocsByUid(db, "PanicEvent", uid)
            deleteDocsByUid(db, "RecentFile", uid)

            val storageRef = storage.reference.child("userFiles/$uid")
            storageRef.listAll()
                .addOnSuccessListener { listResult ->
                    listResult.items.forEach { it.delete() }
                    listResult.prefixes.forEach { folderRef ->
                        folderRef.listAll().addOnSuccessListener { subList ->
                            subList.items.forEach { it.delete() }
                        }
                    }
                    onComplete(true, null)
                }
                .addOnFailureListener { e -> onComplete(false, e.message) }

        }.addOnFailureListener { e ->
            onComplete(false, e.message)
        }
    }

    private fun deleteCollection(db: FirebaseFirestore, path: String) {
        db.collection(path).get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
            }
    }

    private fun deleteDocsByUid(db: FirebaseFirestore, collection: String, uid: String) {
        db.collection(collection).whereEqualTo("uid", uid).get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
            }
    }
}
