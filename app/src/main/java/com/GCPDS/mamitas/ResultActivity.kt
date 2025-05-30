package com.GCPDS.mamitas

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class ResultActivity : AppCompatActivity() {

    private lateinit var imgResult: ImageView
    private lateinit var etMaxTemp: EditText
    private lateinit var etMinTemp: EditText
    private lateinit var btnConfirm: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        imgResult = findViewById(R.id.imgResult)
        etMaxTemp = findViewById(R.id.etMaxTemp)
        etMinTemp = findViewById(R.id.etMinTemp)
        btnConfirm = findViewById(R.id.btnConfirm)

        // Recuperar datos del Intent.
        val imagePath = intent.getStringExtra("imagePath")
        val maxTemp = intent.getStringExtra("max_temp") ?: ""
        val minTemp = intent.getStringExtra("min_temp") ?: ""

        // Mostrar la imagen (opcional).
        imagePath?.let {
            Glide.with(this).load("file://$it").into(imgResult)
        }

        // Prellenar los EditText con los valores extraídos (si existen).
        etMaxTemp.setText(maxTemp)
        etMinTemp.setText(minTemp)

        // Al confirmar, se pueden guardar o procesar los valores ingresados.
        btnConfirm.setOnClickListener {
            val finalMax = etMaxTemp.text.toString()
            val finalMin = etMinTemp.text.toString()
            // Empaquetar resultado y cerrar
            val resultIntent = Intent().apply {
                putExtra("max_temp", finalMax)
                putExtra("min_temp", finalMin)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}



