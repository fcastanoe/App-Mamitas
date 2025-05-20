package com.GCPDS.mamitas

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity


class ImageViewActivity : AppCompatActivity() {
    companion object { const val EXTRA_IMG_PATH = "img_path" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_view)

        val iv = findViewById<ImageView>(R.id.imgViewer)
        val path = intent.getStringExtra(EXTRA_IMG_PATH)!!
        iv.setImageBitmap(BitmapFactory.decodeFile(path))
    }
}