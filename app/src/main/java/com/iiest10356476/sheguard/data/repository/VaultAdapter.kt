package com.iiest10356476.sheguard.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.data.models.FileType
import com.iiest10356476.sheguard.data.models.Vault
import com.iiest10356476.sheguard.data.models.VaultFile
import java.text.SimpleDateFormat
import java.util.*

class VaultAdapter(
    private val vaults: List<Vault>,
    private val onDeleteClick: (VaultFile) -> Unit,
    private val onDownloadClick: (VaultFile) -> Unit
) : RecyclerView.Adapter<VaultAdapter.VaultViewHolder>() {

    // Flatten Vaults -> VaultFile with timestamp
    private val items: List<Pair<Long, VaultFile>> = vaults.flatMap { vault ->
        if (vault.files.isNotEmpty()) {
            vault.files.map { file -> Pair(vault.submitDate, file) }
        } else {
            listOf(Pair(vault.submitDate, VaultFile("No files", FileType.PHOTO)))
        }
    }

    class VaultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView = itemView.findViewById(R.id.file_thumbnail)
        val fileName: TextView = itemView.findViewById(R.id.file_name)
        val fileType: TextView = itemView.findViewById(R.id.file_type)
        val submitDate: TextView = itemView.findViewById(R.id.submit_date)
        val deleteButton: Button = itemView.findViewById(R.id.delete_button)
        val downloadButton: Button = itemView.findViewById(R.id.download_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vault, parent, false)
        return VaultViewHolder(view)
    }

    override fun onBindViewHolder(holder: VaultViewHolder, position: Int) {
        val (timestamp, vaultFile) = items[position]

        holder.fileName.text = vaultFile.url.substringAfterLast("/")
        holder.fileType.text = when (vaultFile.type) {
            FileType.PHOTO -> "Photo"
            FileType.VIDEO -> "Video"
            FileType.AUDIO -> "Audio"
        }
        holder.submitDate.text =
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))

        // Placeholder thumbnail
        holder.thumbnail.setImageResource(android.R.drawable.ic_menu_report_image)

        // Button actions
        holder.deleteButton.setOnClickListener {
            if (vaultFile.url.startsWith("https://")) {
                onDeleteClick(vaultFile)
            }
        }

        holder.downloadButton.setOnClickListener {
            if (vaultFile.url.startsWith("https://")) {
                onDownloadClick(vaultFile)
            }
        }
    }
    override fun getItemCount(): Int = items.size
}
