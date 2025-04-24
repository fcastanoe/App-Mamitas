package com.GCPDS.mamitas

import android.content.Context
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import org.json.JSONObject
import java.io.FileOutputStream

class PlotActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plot)
        enableEdgeToEdge()

        val ivSeg    = findViewById<ImageView>(R.id.imgSegOverlay)
        val ivDerm   = findViewById<ImageView>(R.id.imgDermContours)
        val tempsCont= findViewById<LinearLayout>(R.id.tempsContainer)
        val btnReset = findViewById<Button>(R.id.btnReset)

        // Carga de imágenes y temperaturas (igual que antes)...
        val segPath   = intent.getStringExtra("segOverlayPath") ?: return
        val dermPath  = intent.getStringExtra("dermContourPath") ?: return
        val jsonStr   = intent.getStringExtra("tempsJson")      ?: "{}"

        val noCacheOpts = RequestOptions()
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)

        Glide.with(this)
            .load(File(segPath))
            .apply(noCacheOpts)
            .into(ivSeg)

        Glide.with(this)
            .load(File(dermPath))
            .apply(noCacheOpts)
            .into(ivDerm)

        JSONObject(jsonStr).keys().forEach { key ->
            val value = JSONObject(jsonStr).getDouble(key)
            val tv = TextView(this).apply {
                text = "$key: ${"%.2f".format(value)} °C"
                textSize = 16f
                setPadding(0,8,0,8)
            }
            tempsCont.addView(tv)
        }

        // Cuando el usuario quiera probar con otra imagen, cerrar esta Activity
        btnReset.setOnClickListener {
            finish()
        }
    }
}