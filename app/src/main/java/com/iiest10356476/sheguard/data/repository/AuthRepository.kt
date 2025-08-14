package com.iiest10356476.sheguard.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.iiest10356476.sheguard.data.models.User
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    private val TAG = "AuthRepository"

    // Sign up with email and password
    suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        dateOfBirth: String
    ): Result<User> {
        return try {
            Log.d(TAG, "=== SIGNUP STARTED ===")

            // Create Firebase Auth user
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("User creation failed")

            Log.d(TAG, "✅ Firebase Auth user created!")

            // Create user document in Firestore
            val user = User(
                uid = firebaseUser.uid,
                fullName = fullName,
                email = email,
                dateOfBirth = dateOfBirth
            )

            // Save user data to Firestore
            usersCollection.document(firebaseUser.uid).set(user).await()
            Log.d(TAG, "✅ User saved to Firestore!")

            // Send email verification
            firebaseUser.sendEmailVerification().await()
            Log.d(TAG, "✅ Email verification sent!")

            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "❌ SIGNUP FAILED: ${e.message}")
            Result.failure(e)
        }
    }

    // Sign in with email and password
    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            Log.d(TAG, "=== SIGNIN STARTED ===")

            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Sign in failed")

            Log.d(TAG, "✅ Firebase Auth successful!")

            // Update last login time
            usersCollection.document(firebaseUser.uid)
                .update("lastLoginAt", System.currentTimeMillis())
                .await()

            // Get user data from Firestore
            val userDoc = usersCollection.document(firebaseUser.uid).get().await()
            val user = userDoc.toObject(User::class.java) ?: throw Exception("User profile not found")

            Log.d(TAG, "✅ SIGNIN COMPLETED!")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "❌ SIGNIN FAILED: ${e.message}")
            Result.failure(e)
        }
    }

    // Send password reset email
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sign out
    fun signOut() {
        auth.signOut()
    }

    // Get current user
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    // Check if user is signed in
    fun isUserSignedIn(): Boolean = auth.currentUser != null
}
