package com.iiest10356476.sheguard.data.repository

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.text.toByteArray
import kotlin.to
import kotlin.toString

@RunWith(AndroidJUnit4::class)
class SettingsRepoTest {

    private lateinit var repo: SettingsRepo
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    @Before
    fun setup() = runTest {
        auth = Firebase.auth
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        repo = SettingsRepo()

        // Create a test user and sign in
        val email = "testuser_SettingsRepo@example.com"
        val password = "Test1234!"
        auth.createUserWithEmailAndPassword(email, password).await()
        assertNotNull(auth.currentUser)

        // Add dummy Firestore data for cleanup verification
        val uid = auth.currentUser!!.uid
        firestore.collection("users").document(uid).set(mapOf("name" to "test")).await()
        firestore.collection("EmergencyContact").add(mapOf("uid" to uid, "contact" to "911"))
            .await()

        // Add dummy Storage file
        val storageRef = storage.reference.child("userFiles/$uid/test.txt")
        storageRef.putBytes("hello".toByteArray()).await()
    }

    @After
    fun teardown() = runTest {
        //deleting the user after running so that we can run the test again if need be
        auth.currentUser?.delete()?.await()
    }

    @Test
    fun testDeleteCurrentUserData() = runTest {
        val latch = CompletableDeferred<Pair<Boolean, String?>>()
        //running the delete method and then we will verify that everything has been deleted
        repo.deleteCurrentUserData { success, error ->
            latch.complete(success to error)
        }

        val (success, error) = latch.await()
        assertTrue(success, "Expected success but got error: $error")

        // Verify deletion from Firestore
        val uid = auth.currentUser?.uid ?: return@runTest
        val snapshot = firestore.collection("users").document(uid).get().await()
        assertTrue(!snapshot.exists(), "User doc should be deleted")

        // Verify deletion from Storage
        val storageRef = storage.reference.child("userFiles/$uid/test.txt")
        try {
            storageRef.metadata.await()
            fail("File should have been deleted")
        } catch (ex: Exception) {
            //displaying error in log if an error occurs
            Log.e("Error during settingRepo test", ex.message.toString())
        }
    }
}