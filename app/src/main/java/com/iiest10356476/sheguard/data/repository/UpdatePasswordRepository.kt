package com.iiest10356476.sheguard.data.repository

import android.util.Log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.tasks.await

class UpdatePasswordRepository {

    val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "UpdatePasswordRepo"
    }

    suspend fun updatePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        val user = auth.currentUser
        if (user == null) {
            return Result.failure(Exception("User not logged in."))
        }
        // Thank you, Mr,Delron
        //Get current Credentials
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

        return try {
            Log.d(TAG, "Attempting to re-authenticate user...")

            user.reauthenticate(credential).await()
            Log.d(TAG, " User re-authenticated successfully.")

            // Update password
            user.updatePassword(newPassword).await()
            Log.d(TAG, "✅ Password updated successfully.")

            Result.success(Unit)
        } catch (e: Exception) {
            val message = when (e) {
                is FirebaseAuthRecentLoginRequiredException ->
                    "You must sign in again before updating your password."
                is FirebaseAuthInvalidCredentialsException ->
                    "The current password you entered is incorrect."
                else -> e.message ?: "Unknown error occurred."
            }
            Log.e(TAG, " PASSWORD UPDATE FAILED: $message", e)
            Result.failure(Exception(message))
        }
    }
}
