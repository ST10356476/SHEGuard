package com.iiest10356476.sheguard.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iiest10356476.sheguard.R
import com.iiest10356476.sheguard.data.models.FileType
import com.iiest10356476.sheguard.data.models.VaultFile
import java.text.SimpleDateFormat
import java.util.*

data class RecentFileItem(
    val file: VaultFile,
    val uploadTime: Long
)

class RecentFilesAdapter(
    private val onDeleteClick: (VaultFile) -> Unit,
    private val onDownloadClick: (VaultFile) -> Unit
) : RecyclerView.Adapter<RecentFilesAdapter.ViewHolder>() {

    private var files: MutableList<RecentFileItem> = mutableListOf()
    private val TAG = "RecentFilesAdapter"

    fun updateFiles(newFiles: List<RecentFileItem>) {
        Log.d(TAG, "Updating adapter with ${newFiles.size} files")
        files.clear()
        files.addAll(newFiles)
        notifyDataSetChanged()

        // Log each file for debugging
        files.forEachIndexed { index, item ->
            Log.d(TAG, "File $index: Type=${item.file.type}, URL=${item.file.url}, Time=${item.uploadTime}")
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnailImageView: ImageView? = itemView.findViewById(R.id.thumbnailImageView)
        val fileTypeTextView: TextView? = itemView.findViewById(R.id.fileTypeTextView)
        val uploadTimeTextView: TextView? = itemView.findViewById(R.id.uploadTimeTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d(TAG, "Creating new ViewHolder")
        return try {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_recent_file, parent, false)
            ViewHolder(view)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating ViewHolder, using fallback layout", e)
            // Fallback to a simple TextView if the layout is missing
            val textView = TextView(parent.context).apply {
                text = "Recent File"
                setPadding(16, 16, 16, 16)
            }
            ViewHolder(textView)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position >= files.size) {
            Log.e(TAG, "Invalid position: $position, files size: ${files.size}")
            return
        }

        val item = files[position]
        val file = item.file

        Log.d(TAG, "Binding file at position $position: ${file.type}")

        try {
            // Set file type text and thumbnail based on file type
            when (file.type) {
                FileType.PHOTO -> {
                    holder.fileTypeTextView?.text = "Photo"
                    holder.thumbnailImageView?.setImageResource(android.R.drawable.ic_menu_camera)
                    Log.d(TAG, "Set photo icon for position $position")
                }
                FileType.VIDEO -> {
                    holder.fileTypeTextView?.text = "Video"
                    holder.thumbnailImageView?.setImageResource(android.R.drawable.ic_menu_slideshow)
                    Log.d(TAG, "Set video icon for position $position")
                }
                FileType.AUDIO -> {
                    holder.fileTypeTextView?.text = "Audio"
                    holder.thumbnailImageView?.setImageResource(android.R.drawable.ic_btn_speak_now)
                    Log.d(TAG, "Set audio icon for position $position")
                }
                FileType.DOCUMENTS -> {
                    holder.fileTypeTextView?.text = "Document"
                    holder.thumbnailImageView?.setImageResource(android.R.drawable.ic_menu_info_details)  // Better icon for documents
                    Log.d(TAG, "Set document icon for position $position")
                }
            }

            // Format upload time
            val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            val formattedTime = if (item.uploadTime > 0) {
                dateFormat.format(Date(item.uploadTime))
            } else {
                "Unknown"
            }
            holder.uploadTimeTextView?.text = formattedTime

            Log.d(TAG, "Set time for position $position: $formattedTime")

            // Set click listeners
            holder.itemView.setOnClickListener {
                Log.d(TAG, "Item clicked at position $position")
                onDownloadClick(file)
            }

            holder.itemView.setOnLongClickListener {
                Log.d(TAG, "Item long-pressed at position $position")
                onDeleteClick(file)
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error binding ViewHolder at position $position", e)
        }
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount() returning: ${files.size}")
        return files.size
    }
}