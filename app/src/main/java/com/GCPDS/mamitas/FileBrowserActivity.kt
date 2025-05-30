package com.GCPDS.mamitas

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import org.json.JSONObject

class FileBrowserActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PATH = "extra_path"
        const val EXTRA_SHOW_FILES = "extra_show_files"  // true = mostrar archivos, false = solo carpetas
    }

    private lateinit var rv: RecyclerView
    private lateinit var currentDir: File
    private var showFiles = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_browser)

        rv = findViewById(R.id.rvBrowser)
        rv.layoutManager = LinearLayoutManager(this)

        // Recuperamos ruta y flag
        val path = intent.getStringExtra(EXTRA_PATH)!!
        currentDir = File(path)
        showFiles = intent.getBooleanExtra(EXTRA_SHOW_FILES, false)

        title = currentDir.name
        listDirectory()
    }

    private fun listDirectory() {
        val items = if (showFiles) {
            currentDir.listFiles { f -> f.isFile }?.map { it.name } ?: emptyList()
        } else {
            currentDir.listFiles { f -> f.isDirectory }?.map { it.name } ?: emptyList()
        }
        rv.adapter = FileAdapter(
            items    = items,
            baseDir  = currentDir,
            showFiles = showFiles,
            onClick  = { f ->
                if (f.isDirectory) {
                    // Navegar a subcarpeta
                    val isTn = showFiles || f.name.matches(Regex("""t\d+"""))
                    startActivity(Intent(this, FileBrowserActivity::class.java).apply {
                        putExtra(EXTRA_PATH, f.absolutePath)
                        putExtra(EXTRA_SHOW_FILES, isTn)
                    })
                } else {
                    when (f.extension) {
                        "json" -> {
                            // Parse y mostrar listado
                            val obj = JSONObject(f.readText())
                            val entries = obj.keys().asSequence()
                                .map { key -> "$key: ${"%.2f".format(obj.getDouble(key))} °C" }
                                .toList()
                            AlertDialog.Builder(this, R.style.AlertDialogCustom)
                                .setTitle("Temperaturas")
                                .setItems(entries.toTypedArray(), null)
                                .setPositiveButton("OK", null)
                                .show()
                        }
                        "png", "jpg" -> {
                            // Lanzar visor de imagen
                            startActivity(Intent(this, ImageViewActivity::class.java)
                                .putExtra(ImageViewActivity.EXTRA_IMG_PATH, f.absolutePath))
                        }
                    }
                }
            }
        )
    }
}
