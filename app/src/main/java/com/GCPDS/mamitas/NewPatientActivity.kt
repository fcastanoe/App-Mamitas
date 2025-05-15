package com.GCPDS.mamitas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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
            val data = Intent().putExtra("newPatient", patient)
            setResult(RESULT_OK, data)
            finish()
        }
    }
}
