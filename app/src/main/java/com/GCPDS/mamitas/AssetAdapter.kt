package com.GCPDS.mamitas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AssetAdapter(
    private val items: List<String>,
    private val assetBase: String,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<AssetAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val ivIcon = view.findViewById<ImageView>(R.id.ivIcon)
        private val tvName = view.findViewById<TextView>(R.id.tvName)

        fun bind(name: String) {
            val context = itemView.context
            val mgr = context.assets
            val fullPath = "$assetBase/$name"
            //¿Tiene hijos? → directorio
            val children = mgr.list(fullPath)
            val isDir = children != null && children.isNotEmpty()

            // Icono según tipo
            ivIcon.setImageResource(
                if (isDir) R.drawable.ic_folder
                else R.drawable.ic_image_search
            )
            tvName.text = name

            itemView.setOnClickListener {
                onClick(name)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}