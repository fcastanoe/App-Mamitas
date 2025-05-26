package com.GCPDS.mamitas

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.io.File


class ImageViewActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_IMG_PATH = "extra_img_path"
        const val EXTRA_FROM_ASSETS = "extra_from_assets"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Asume que tu layout tiene un ImageView con id "ivFullImage"
        setContentView(R.layout.activity_image_view)

        val iv = findViewById<ImageView>(R.id.imgViewer)
        val imgPath = intent.getStringExtra(EXTRA_IMG_PATH)!!
        val fromAssets = intent.getBooleanExtra(EXTRA_FROM_ASSETS, false)

        if (fromAssets) {
            // Lee el InputStream desde assets y decodifica
            assets.open(imgPath).use { input ->
                val bmp = BitmapFactory.decodeStream(input)
                iv.setImageBitmap(bmp)
            }
        } else {
            // Carga normal desde fichero
            Glide.with(this)
                .load(File(imgPath))
                .into(iv)
        }
    }
}