package com.GCPDS.mamitas

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException

class AssetBrowserActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ASSET_PATH   = "extra_asset_path"
        const val EXTRA_FROM_ASSETS  = "extra_from_assets"
    }

    private lateinit var currentPath: String
    private lateinit var rv: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_browser)  // Reusa este layout

        rv = findViewById(R.id.rvBrowser)
        rv.layoutManager = LinearLayoutManager(this)

        currentPath = intent.getStringExtra(EXTRA_ASSET_PATH)!!
        title       = currentPath.substringAfterLast('/')

        listAssets(currentPath)
    }

    private fun listAssets(path: String) {
        val mgr     = assets
        val entries = try {
            mgr.list(path)?.toList() ?: emptyList()
        } catch (e: IOException) {
            emptyList<String>()
        }

        rv.adapter = AssetAdapter(entries, path) { name ->
            val fullPath = "$path/$name"
            // Si al listar vemos que tiene hijos => carpeta
            val isDir = try {
                assets.list(fullPath)?.isNotEmpty() == true
            } catch (_: IOException) { false }

            if (isDir) {
                // navegar más profundo
                startActivity(Intent(this, AssetBrowserActivity::class.java).apply {
                    putExtra(EXTRA_ASSET_PATH, fullPath)
                })
            } else {
                // es un fichero de imagen: lanzar ImageViewActivity indicando que viene de assets
                startActivity(Intent(this, ImageViewActivity::class.java).apply {
                    putExtra(ImageViewActivity.EXTRA_IMG_PATH, fullPath)
                    putExtra(ImageViewActivity.EXTRA_FROM_ASSETS, true)
                })
            }
        }
    }
}