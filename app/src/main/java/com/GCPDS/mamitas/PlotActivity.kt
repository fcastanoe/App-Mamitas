package com.GCPDS.mamitas

import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File
import android.widget.ImageView
import java.io.FileOutputStream

class PlotActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_plot)

        val imgPlot = findViewById<ImageView>(R.id.imgPlot)

        val plotPath = intent.getStringExtra("plotPath") ?: return

        // Carga el PNG generado
        Glide.with(this)
            .load("file://${plotPath}")
            .into(imgPlot)
    }

    // Puedes copiar aquí el mismo método copyModelFile() de tu MamitasAppActivity
    private fun copyModelFile(context: Context) {
        val assetManager = context.assets
        val modelDir = File(context.filesDir, "models")
        if (!modelDir.exists()) modelDir.mkdirs()
        val outFile = File(modelDir, "ResUNet_efficientnetb3_Mamitas.tflite")
        if (!outFile.exists()) {
            assetManager.open("models/ResUNet_efficientnetb3_Mamitas.tflite").use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}