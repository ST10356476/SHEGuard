package com.iiest10356476.sheguard.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.data.models.Vault
import com.iiest10356476.sheguard.data.repository.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SecureVaultViewAll : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val vaultRepository = VaultRepository()
    private var vaultItems: List<Vault> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_secure_vault_view_all)

        recyclerView = findViewById(R.id.files_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Apply system insets (status/nav bar padding)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loadVaultItems()
    }

    private fun loadVaultItems() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        CoroutineScope(Dispatchers.Main).launch {
            vaultItems = vaultRepository.getRecentVaultItems(uid)
            recyclerView.adapter = VaultAdapter(
                vaultItems,
                onDeleteClick = { file ->
                    CoroutineScope(Dispatchers.Main).launch {
                        vaultRepository.deleteFile(file)
                        loadVaultItems()
                    }
                },
                onDownloadClick = { file ->
                    vaultRepository.getDownloadUrl(file) { url ->
                        url?.let { Log.d("SecureVaultViewAll", "Download URL: $it") }
                    }
                }
            )
        }
    }
}
