package com.GCPDS.mamitas

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class FolderActivity : AppCompatActivity() {

    private lateinit var patient: Patient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder)

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbarFolder)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_details) {
                // Muestra detalles idénticos al dialog de FormularioActivity
                AlertDialog.Builder(this)
                    .setTitle("Detalles de paciente")
                    .setMessage(
                        "Nombre: ${patient.first} ${patient.last}\n" +
                                "Edad: ${patient.age}\n" +
                                "Peso: ${patient.weight}\n" +
                                "Estatura: ${patient.height}"
                    )
                    .setPositiveButton("OK", null)
                    .show()
                true
            } else false
        }

        // Recupera datos
        patient = intent.getParcelableExtra("patient")!!

        // Aquí podrías listar archivos de la carpeta si los tienes:
        // val folder = File(filesDir, "${patient.first}_${patient.last}")
        // ...
    }
}
