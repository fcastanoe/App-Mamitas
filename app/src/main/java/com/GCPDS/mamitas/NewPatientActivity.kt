package com.GCPDS.mamitas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class NewPatientActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_patient)

        val etF = findViewById<EditText>(R.id.etFirstName)
        val etL = findViewById<EditText>(R.id.etLastName)
        val etAge = findViewById<EditText>(R.id.etAge)
        val etW = findViewById<EditText>(R.id.etWeight)
        val etH = findViewById<EditText>(R.id.etHeight)
        val btn = findViewById<Button>(R.id.btnConfirm)

        val existing = intent.getParcelableExtra<Patient>("editPatient")

        if (existing != null) {
            etF.setText(existing.first)
            etL.setText(existing.last)
            etAge.setText(existing.age.toString())
            etW.setText(existing.weight.toString())
            etH.setText(existing.height.toString())
        }

        btn.setOnClickListener {
            val first = etF.text.toString().trim()
            val last  = etL.text.toString().trim()
            if (first.isEmpty() || last.isEmpty()) {
                Toast.makeText(this, "Nombre y apellido obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val patient = Patient(
                first, last,
                etAge.text.toString().toIntOrNull() ?: 0,
                etW.text.toString().toFloatOrNull() ?: 0f,
                etH.text.toString().toFloatOrNull() ?: 0f
            )

            // Recupero el nombre de carpeta original que me pasó FormularioActivity (si viene)
            val originalFolder = intent.getStringExtra("originalFolderName")

            val data = Intent().putExtra("newPatient", patient)
            if (originalFolder != null) {
                data.putExtra("originalFolderName", originalFolder)
            }
            setResult(RESULT_OK, data)
            finish()
        }
    }
}
