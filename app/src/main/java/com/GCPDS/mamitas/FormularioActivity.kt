package com.GCPDS.mamitas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.os.Parcelable
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import kotlinx.parcelize.Parcelize
import java.io.File

class FormularioActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var rvPatients: RecyclerView
    private val patients = mutableListOf<Patient>()
    private val prefs by lazy { getSharedPreferences("app_prefs", MODE_PRIVATE) }
    private val gson = com.google.gson.Gson()

    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_formulario)

        drawerLayout = findViewById(R.id.drawer_layout)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)

        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()


        navView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        // cargar pacientes guardados de prefs
        prefs.getStringSet("patients", emptySet())!!
            .forEach { folderName ->
                // aquí asumimos sólo que el nombre de carpeta es "First_Last"
                prefs.getString("patient_$folderName", null)?.let { json ->
                    patients.add(gson.fromJson(json, Patient::class.java))
                    }
            }

        val btnCreate = findViewById<Button>(R.id.btnCreate)
        rvPatients = findViewById(R.id.rvPatients)

        // Configuro RecyclerView
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
                AlertDialog.Builder(this, R.style.AlertDialogCustom)
                    .setTitle("Paciente: ${patient.first} ${patient.last}")
                    .setMessage("Edad: ${patient.age}\n" +
                                "Peso: ${patient.weight} Kg\n" +
                                "Estatura: ${patient.height} cm"
                    )
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Edit") { _, _ ->
                        // Cuando pulso Edit, envío también el nombre de carpeta original
                        val originalFolder = "${patient.first}_${patient.last}"
                        val intent = Intent(this, NewPatientActivity::class.java).apply {
                            putExtra("editPatient", patient)
                            putExtra("originalFolderName", originalFolder)
                        }
                            startActivityForResult(intent, REQUEST_EDIT_PATIENT)
                    }
                        .setNegativeButton("Delete") { _, _ ->
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

        // Botón Crear paciente
        btnCreate.setOnClickListener {
            Intent(this, NewPatientActivity::class.java).let {
                startActivityForResult(it, REQUEST_NEW_PATIENT)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Esto permite que el toggle (la “hamburguesa”) abra/cierre el drawer
        if (toggle.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_inicio      -> startActivity(Intent(this, MainActivity::class.java))
            R.id.nav_formulario  -> { /* ya estás */ }
            R.id.nav_imagenes    -> startActivity(Intent(this, MamitasAppActivity::class.java))
            R.id.nav_resultados  -> startActivity(Intent(this, ResultadosActivity::class.java))
            R.id.nav_basededatos -> startActivity(Intent(this, BaseDeDatosActivity::class.java))
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == REQUEST_NEW_PATIENT || requestCode == REQUEST_EDIT_PATIENT)
            && resultCode == RESULT_OK && data != null) {

            val patient = data.getParcelableExtra<Patient>("newPatient")
            patient?.let {
                // Carpeta “nueva” que queremos tenga: First_Last
                val newFolderName = "${it.first}_${it.last}"

                // Si vino de un EDIT, data tendrá "originalFolderName"
                val originalFolderName = data.getStringExtra("originalFolderName")

                if (requestCode == REQUEST_EDIT_PATIENT && originalFolderName != null) {
                    // Caso RENOMBRADO: mover (rename) carpeta antigua → nueva
                    if (originalFolderName != newFolderName) {
                        val oldDir = File(filesDir, originalFolderName)
                        val newDir = File(filesDir, newFolderName)
                        if (oldDir.exists()) {
                            // 1) Intentamos renombrar (mover) la carpeta física
                            val moved = oldDir.renameTo(newDir)
                            if (!moved) {
                                // Si falla el renameTo (p. ej. en algunos dispositivos), podemos hacer copy + delete
                                oldDir.copyRecursively(newDir, overwrite = true)
                                oldDir.deleteRecursively()
                            }
                        }
                        // 2) Actualizamos SharedPreferences: quitamos la entrada antigua
                        val setOld = prefs.getStringSet("patients", emptySet())!!.toMutableSet()
                        setOld.remove(originalFolderName)
                        prefs.edit()
                            .putStringSet("patients", setOld)
                            .remove("patient_$originalFolderName")
                            .apply()

                        // 3) En la lista `patients`, buscar índice del Patient con la carpeta antigua
                        val oldIndex = patients.indexOfFirst { p ->
                            "${p.first}_${p.last}" == originalFolderName
                        }
                        if (oldIndex >= 0) {
                            // elimina el paciente antiguo de la lista y notifica al adapter
                            patients.removeAt(oldIndex)
                            rvPatients.adapter?.notifyItemRemoved(oldIndex)
                        }
                        // (no hace falta volver a borrar subcarpetas: ya están dentro de newDir)
                    }
                }

                // Ahora procedo a “guardar/actualizar” como si fuera nuevo
                // (si renombré, la lista ya eliminó al viejo, así que aquí simplemente lo añado;
                //  si NO renombré, quizá ya exista en la lista: en ese caso lo sobreescribo en su posición)

                // Asegurarme de que existe la carpeta nueva
                File(filesDir, newFolderName).apply { if (!exists()) mkdirs() }

                // Guardo o actualizo prefs con la carpeta nueva
                val set2 = prefs.getStringSet("patients", emptySet())!!.toMutableSet()
                set2.add(newFolderName)
                prefs.edit()
                    .putStringSet("patients", set2)
                    .putString("patient_$newFolderName", gson.toJson(it))
                    .apply()

                // Refrescar lista en RecyclerView:
                // - Si ya existía (por edad/peso/estatura), reemplazo en el mismo índice
                // - Si no existía (o se renombró, o es creación), lo añado al final

                // Intento hallar índice basándome en carpeta “nueva”
                val newIndex = patients.indexOfFirst { p ->
                    "${p.first}_${p.last}" == newFolderName
                }
                if (newIndex >= 0) {
                    // Ya existía (cambios en edad/peso/estatura); actualizo
                    patients[newIndex] = it
                    rvPatients.adapter?.notifyItemChanged(newIndex)
                } else {
                    // No existía: lo agrego como nuevo al final
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
