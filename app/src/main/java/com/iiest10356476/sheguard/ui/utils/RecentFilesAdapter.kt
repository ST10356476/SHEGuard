//package com.iiest10356476.sheguard.ui.adapter
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import androidx.recyclerview.widget.RecyclerView
//import com.bumptech.glide.Glide
//import com.iiest10356476.sheguard.R
//
//class RecentFilesAdapter(private val items: List<String>) :
//    RecyclerView.Adapter<RecentFilesAdapter.FileViewHolder>() {
//
//    inner class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val thumbnail: ImageView = view.findViewById(R.id.file_thumbnail)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_recent_file, parent, false)
//        return FileViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
//        val url = items[position]
//
//        // For images, load thumbnail; for video/audio, show placeholder
//        if (url.endsWith(".jpg") || url.endsWith(".png") || url.endsWith(".jpeg")) {
//            Glide.with(holder.thumbnail.context).load(url).into(holder.thumbnail)
//        } else if (url.endsWith(".mp4")) {
//            holder.thumbnail.setImageResource(R.drawable.ic_video_placeholder)
//        } else if (url.endsWith(".mp3")) {
//            holder.thumbnail.setImageResource(R.drawable.ic_audio_placeholder)
//        } else {
//            holder.thumbnail.setImageResource(R.drawable.ic_file_placeholder)
//        }
//    }
//
//    override fun getItemCount(): Int = items.size
//}
