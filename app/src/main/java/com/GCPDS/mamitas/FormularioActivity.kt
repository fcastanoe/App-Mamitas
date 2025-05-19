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
    private val prefs by lazy { getSharedPreferences("app_prefs", MODE_PRIVATE) }
    private val gson = com.google.gson.Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_formulario)

        // ADD: cargar pacientes guardados de prefs
        prefs.getStringSet("patients", emptySet())!!
            .forEach { folderName ->
                // aquí asumimos sólo que el nombre de carpeta es "First_Last"
                prefs.getString("patient_$folderName", null)?.let { json ->
                    patients.add(gson.fromJson(json, Patient::class.java))
                    }
            }

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
                // Muestra diálogo con Info, Modificar y Eliminar
                AlertDialog.Builder(this)
                    .setTitle("Paciente: ${patient.first} ${patient.last}")
                    .setMessage(
                        "Edad: ${patient.age}\n" +
                                "Peso: ${patient.weight} kg\n" +
                                "Estatura: ${patient.height} cm"
                    )
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Modificar") { _, _ ->
                        // Lanzar NewPatientActivity para editar
                        Intent(this, NewPatientActivity::class.java).also {
                            it.putExtra("editPatient", patient)
                        }.let {
                            startActivityForResult(it, REQUEST_EDIT_PATIENT)
                        }
                    }
                        .setNegativeButton("Eliminar") { _, _ ->
                            // Borrar carpeta y prefs
                            val fname = "${patient.first}_${patient.last}"
                            File(filesDir, fname).deleteRecursively()
                            val set = prefs.getStringSet("patients", emptySet())!!.toMutableSet()
                            set.remove(fname)
                            prefs.edit().putStringSet("patients", set).remove("patient_$fname").apply()
                            // Actualiza lista
                            val idx = patients.indexOf(patient)
                            patients.removeAt(idx)
                            rvPatients.adapter?.notifyItemRemoved(idx)
                        }.show()
            }
        )
        rvPatients.adapter = adapter

        // 2) Botón Crear paciente
        btnCreate.setOnClickListener {
            Intent(this, NewPatientActivity::class.java).let {
                startActivityForResult(it, REQUEST_NEW_PATIENT)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == REQUEST_NEW_PATIENT || requestCode == REQUEST_EDIT_PATIENT)
            && resultCode == RESULT_OK && data != null) {
            val patient = data.getParcelableExtra<Patient>("newPatient")
            patient?.let {
                // Crea carpeta física
                val folderName = "${it.first}_${it.last}"
                File(filesDir, folderName).apply { if (!exists()) mkdirs() }

                // ADD: guardar en prefs
                val set = prefs.getStringSet("patients", emptySet())!!.toMutableSet()
                set.add(folderName)
                prefs.edit()
                    .putStringSet("patients", set)
                    .putString("patient_$folderName", gson.toJson(it))
                    .apply()

                // Añade a la lista y notifica
                val idx = patients.indexOfFirst { p ->
                    "${p.first}_${p.last}" == folderName
                }
                if (idx >= 0) {
                    patients[idx] = it
                    rvPatients.adapter?.notifyItemChanged(idx)
                } else {
                    patients.add(it)
                    rvPatients.adapter?.notifyItemInserted(patients.size - 1)
                }
            }
        }
    }

    companion object {
        const val REQUEST_NEW_PATIENT = 1001
        const val REQUEST_EDIT_PATIENT = 1002
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
