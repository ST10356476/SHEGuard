package com.iiest10356476.sheguard

import android.net.Uri
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.iiest10356476.sheguard.data.models.FileType
import com.iiest10356476.sheguard.data.models.Vault
import com.iiest10356476.sheguard.data.models.VaultFile
import com.iiest10356476.sheguard.data.repository.VaultRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class VaultRepositoryIntegrationTest {

    private lateinit var repo: VaultRepository
    private lateinit var auth: FirebaseAuth
    private lateinit var testUid: String

    init {
        runBlocking {
            try {
                auth = FirebaseAuth.getInstance()
                repo = VaultRepository()
                var result = auth.signInWithEmailAndPassword("joshuaPonquett@gmail.com", "TestAccount@123").await()
                testUid = result.user?.uid ?: error("Failed to get user UID")
            } catch (e: Exception) {
               Log.d("Test Login Error: ", "User doesnt exist?")
            }
        }
    }

    @Test
    fun uploadVault_withTempFile_uploadsSuccessfully() = runTest {
        val uploadedFiles = mutableListOf<VaultFile>()

        try {
            // Create temp file
            val tmpFile = File.createTempFile("vault_test", ".jpg")
            tmpFile.writeText("dummy image content") // ensure file is non-empty
            val uri = Uri.fromFile(tmpFile)

            // Upload
            val result = repo.uploadVault(photos = listOf(uri))
            assertTrue(result.isSuccess)

            val vault: Vault = result.getOrThrow()
            uploadedFiles += vault.files

            // Assertions
            assertTrue(vault.files.isNotEmpty())
            assertEquals(FileType.PHOTO, vault.files.first().type)
            assertTrue("Download URL should be valid", vault.files.first().url.startsWith("https://"))
        } finally {
            // Cleanup all uploaded files
            uploadedFiles.forEach { repo.deleteFile(it) }
        }
    }

    @Test
    fun deleteFile_removesFromVault() = runTest {
        // upload one file
        val tmpFile = File.createTempFile("delete_test", ".jpg")
        tmpFile.writeText("to delete")
        val uri = Uri.fromFile(tmpFile)
        val vault = repo.uploadVault(photos = listOf(uri)).getOrThrow()
        val fileToDelete = vault.files.first()

        // delete
        val deleteResult = repo.deleteFile(fileToDelete)
        assertTrue(deleteResult.isSuccess)

        val afterDelete = repo.getRecentVaultItems(testUid)
        val stillExists = afterDelete.any { v -> v.files.any { it.url == fileToDelete.url } }
        assertFalse("File should be deleted from Firestore + Storage", stillExists)
    }

    @Test
    fun getVaultStatistics_countsCorrectly() = runTest {
        val uploadedFiles = mutableListOf<VaultFile>()

        try {
            // Upload photo
            val photoFile = File.createTempFile("stat_photo", ".jpg")
            photoFile.writeText("photo content")
            val photoUri = Uri.fromFile(photoFile)
            uploadedFiles += repo.uploadVault(photos = listOf(photoUri)).getOrThrow().files

            // Upload video
            val videoFile = File.createTempFile("stat_video", ".mp4")
            videoFile.writeText("video content")
            val videoUri = Uri.fromFile(videoFile)
            uploadedFiles += repo.uploadVault(videos = listOf(videoUri)).getOrThrow().files

            val stats = repo.getVaultStatistics(testUid)

            assertEquals(2, stats.totalFiles)
            assertEquals(1, stats.photoCount)
            assertEquals(1, stats.videoCount)
            assertTrue("lastUpload should be > 0", stats.lastUpload > 0)
        } finally {
            // Cleanup all uploaded files
            uploadedFiles.forEach { repo.deleteFile(it) }
        }
    }
}