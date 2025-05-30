package com.GCPDS.mamitas

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class SimpleStringAdapter(
    private val items: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<SimpleStringAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tv = view.findViewById<TextView>(android.R.id.text1)
        fun bind(text: String) {
            tv.text = text
            itemView.setOnClickListener { onClick(text) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}

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
                AlertDialog.Builder(this, R.style.AlertDialogCustom)
                    .setTitle("Detalles de paciente")
                    .setMessage("Nombre: ${patient.first} ${patient.last}\n" +
                                "Edad: ${patient.age}\n" +
                                "Peso: ${patient.weight} Kg\n" +
                                "Estatura: ${patient.height} cm"
                    )
                    .setPositiveButton("OK", null)
                    .show()
                true
            } else false
        }

        // Recupera datos
        patient = intent.getParcelableExtra("patient")!!

        val rv = findViewById<RecyclerView>(R.id.rvSubfolders)
        rv.layoutManager = LinearLayoutManager(this)
        val baseDir = File(filesDir, "${patient.first}_${patient.last}")
        val subs = baseDir.listFiles { f -> f.isDirectory }?.map { it.name } ?: emptyList()
        // Usa FileAdapter en lugar de SimpleStringAdapter
        rv.adapter = FileAdapter(
            items    = subs,
            baseDir  = baseDir,
            showFiles = false,          // aquí solo carpetas
            onClick  = { f ->
                when (f.name) {
                    "Grafica" -> {
                        // 1) Si es la carpeta Grafica, lanza la ChartActivity
                        startActivity(Intent(this, ChartActivity::class.java).apply {
                            putExtra(ChartActivity.EXTRA_PATIENT_FOLDER, baseDir.absolutePath)
                        })
                    }
                    else -> {
                        // 2) Temperaturas / Imagenes o sub-carpeta tN: abre FileBrowserActivity
                        val isTn = f.name.matches(Regex("""t\d+"""))
                        startActivity(Intent(this, FileBrowserActivity::class.java).apply {
                            putExtra(FileBrowserActivity.EXTRA_PATH, f.absolutePath)
                            putExtra(FileBrowserActivity.EXTRA_SHOW_FILES, isTn)
                        })
                    }
                }
            }
        )
    }

}
