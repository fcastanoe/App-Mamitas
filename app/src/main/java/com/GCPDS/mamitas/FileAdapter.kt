package com.GCPDS.mamitas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FileAdapter(
    private val items: List<String>,
    private val baseDir: File,
    private val showFiles: Boolean,
    private val onClick: (File) -> Unit
) : RecyclerView.Adapter<FileAdapter.VH>() {

    inner class VH(item: View) : RecyclerView.ViewHolder(item) {
        private val ivIcon: ImageView = item.findViewById(R.id.ivIcon)
        private val tvName: TextView = item.findViewById(R.id.tvName)
        fun bind(name: String) {
            val f = File(baseDir, name)
            tvName.text = name

            // Icono de carpeta o archivo
            ivIcon.setImageResource(
                if (f.isDirectory) R.drawable.ic_folder else R.drawable.ic_file
            )
            itemView.setOnClickListener { onClick(f) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(items[position])

    override fun getItemCount() = items.size
}