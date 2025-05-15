package com.GCPDS.mamitas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

class FormularioActivity : AppCompatActivity() {

    private lateinit var rvPatients: RecyclerView
    private val patients = mutableListOf<Patient>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_formulario)

        val btnCreate = findViewById<Button>(R.id.btnCreate)
        rvPatients = findViewById(R.id.rvPatients)

        // 1) Configuro RecyclerView
        rvPatients.layoutManager = LinearLayoutManager(this)
        val adapter = PatientAdapter(
            patients,
            onItemClick = { patient ->
                // Abre FolderActivity pasando el objeto patient
                Intent(this, FolderActivity::class.java).also {
                    it.putExtra("patient", patient)
                    startActivity(it)
                }
            },
            onOptionsClick = { patient ->
                // Muestra diálogo de detalles
                AlertDialog.Builder(this)
                    .setTitle("Detalles")
                    .setMessage(
                        "Nombre: ${patient.first} ${patient.last}\n" +
                                "Edad: ${patient.age}\n" +
                                "Peso: ${patient.weight}\n" +
                                "Estatura: ${patient.height}"
                    )
                    .setPositiveButton("OK", null)
                    .show()
            }
        )
        rvPatients.adapter = adapter

        // 2) Botón Crear paciente
        btnCreate.setOnClickListener {
            startActivityForResult(
                Intent(this, NewPatientActivity::class.java),
                REQUEST_NEW_PATIENT
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_NEW_PATIENT && resultCode == RESULT_OK && data != null) {
            val patient = data.getParcelableExtra<Patient>("newPatient")
            patient?.let {
                // Crea carpeta física
                val folderName = "${it.first}_${it.last}"
                File(filesDir, folderName).apply { if (!exists()) mkdirs() }

                // Añade a la lista y notifica
                patients.add(it)
                rvPatients.adapter?.notifyItemInserted(patients.size - 1)
            }
        }
    }

    companion object {
        const val REQUEST_NEW_PATIENT = 1001
    }
}

// --- Modelo Parcelable ---
@Parcelize
data class Patient(
    val first: String,
    val last: String,
    val age: Int,
    val weight: Float,
    val height: Float
) : Parcelable
