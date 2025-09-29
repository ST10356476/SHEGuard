package com.iiest10356476.sheguard.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.iiest10356476.sheguard.data.models.User
import kotlinx.coroutines.tasks.await

class UpdateUserDataRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "UpdateUserDataRepo"
    }

    private fun userDocRef() = auth.currentUser?.uid?.let {
        firestore.collection("users").document(it)
    }

    suspend fun getUserProfile(): Result<User> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("User not logged in."))

        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            val user = snapshot.toObject(User::class.java)
                ?: return Result.failure(Exception("User profile not found."))
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, " Failed to load user profile", e)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(updates: Map<String, Any>): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("User not logged in."))

        return try {
            firestore.collection("users").document(uid).update(updates).await()
            Log.d(TAG, " User profile updated successfully.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, " Failed to update user profile", e)
            Result.failure(e)
        }
    }
}
