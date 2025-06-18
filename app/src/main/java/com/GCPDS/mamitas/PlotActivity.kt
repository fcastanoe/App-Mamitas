package com.GCPDS.mamitas

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.io.File
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import org.json.JSONObject
import android.graphics.Typeface
import android.graphics.Color
import androidx.core.content.FileProvider
import java.io.FileOutputStream

class PlotActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var toggle: ActionBarDrawerToggle
    private val prefs by lazy { getSharedPreferences("app_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plot)

        // 1) Toolbar + Drawer
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        toggle = ActionBarDrawerToggle(
            this, drawer, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()
        navView.setNavigationItemSelectedListener(this)

        // 2) Carga de imagen y lista de temperaturas
        val ivDerm   = findViewById<ImageView>(R.id.imgDermContours)
        val tempsCont= findViewById<LinearLayout>(R.id.tempsContainer)
        val btnSave  = findViewById<Button>(R.id.btnSave)
        val btnReset = findViewById<Button>(R.id.btnReset)

        // Opciones para Glide (sin cache)
        val noCacheOpts = RequestOptions()
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)

        intent.getStringExtra("dermColoredPath")?.let { path ->
            Glide.with(this)
                .load(File(path))
                .apply(noCacheOpts)
                .into(ivDerm)
        }

        intent.getStringExtra("tempsJson")?.let { jsonStr ->
            JSONObject(jsonStr).keys().forEach { key ->
                val value = JSONObject(jsonStr).getDouble(key)
                val tv = TextView(this).apply {
                    text = "$key: ${"%.2f".format(value)} °C"
                    textSize = 16f
                    setTypeface(null, Typeface.BOLD)
                    setPadding(0,8,0,8)
                    setBackgroundColor(Color.parseColor("#80FFFFFF"))
                }
                tempsCont.addView(tv)
            }
        }

        // 3) Botón Guardar → FormularioActivity
        btnSave.setOnClickListener {
            showPatientPicker()
        }

        // 4) Botón Nueva imagen (igual que antes)
        btnReset.setOnClickListener {
            finish()  // cierra PlotActivity para seleccionar otra
        }
    }

    /**
     * Muestra un AlertDialog con:
     *  - Lista de pacientes (nombres de carpeta en filesDir)
     *  - Opción “Crear nuevo paciente”
     */
    private fun showPatientPicker() {
        // 1) Obtenemos carpetas de pacientes
        val patientDirs = prefs.getStringSet("patients", emptySet())!!
                   .toMutableList()
        patientDirs.add("Crear nuevo paciente")

        // 2) Creamos el diálogo
        AlertDialog.Builder(this, R.style.AlertDialogCustom)
            .setTitle("Selecciona paciente")
            .setItems(patientDirs.toTypedArray()) { dialog, which ->
                val choice = patientDirs[which]
                if (choice == "Crear nuevo paciente") {
                    // Lanzamos NewPatientActivity
                    startActivityForResult(
                        Intent(this, NewPatientActivity::class.java),
                        REQUEST_NEW_PATIENT_SAVE
                    )
                } else {
                    // Directamente guardamos en el paciente elegido
                    saveToPatient(choice)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Capturamos también el resultado de crear paciente desde aquí
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_NEW_PATIENT_SAVE && resultCode == RESULT_OK && data != null) {
            val patient = data.getParcelableExtra<Patient>("newPatient")!!
            // Creamos carpeta física en filesDir
            val folderName = "${patient.first}_${patient.last}"
            File(filesDir, folderName).apply { if (!exists()) mkdirs() }
            // ADD: guardar en prefs igual que en FormularioActivity
            val set = prefs.getStringSet("patients", emptySet())!!.toMutableSet()
            set.add(folderName)
            prefs.edit()
                .putStringSet("patients", set)
                .putString("patient_$folderName", com.google.gson.Gson().toJson(patient))
                .apply()
            // Ahora guardamos en ella
            saveToPatient(folderName)
        }
        // También déjalo para el caso de volver de Nuevo paciente en FormularioActivity
        // y el REQUEST_NEW_PATIENT original si lo necesitas
    }

    /**
     * Se encarga de:
     *  - Crear subcarpetas Temperaturas/, Imagenes/, Grafica/ (si no existen)
     *  - Dentro de ambas, contar cuántos tN ya hay, crear t<next>/
     *  - Guardar JSON de temperaturas en Temperaturas/tN/data.json
     *  - Guardar la imagen ploteada en Imagenes/tN/image.png
     *  - No tocar Grafica/ (se deja vacía)
     *  - Mostrar Toast y cerrar diálogo (o activity)
     */
    private fun saveToPatient(patientFolder: String) {
        // 1) Rutas base
        val baseDir = File(filesDir, patientFolder)
        val tempsDir = File(baseDir, "Temperaturas").apply { if (!exists()) mkdirs() }
        val imgsDir  = File(baseDir, "Imagenes").apply { if (!exists()) mkdirs() }
        val regsDir      = File(baseDir, "Registros").apply    { if (!exists()) mkdirs() }
        val graphDir = File(baseDir, "Grafica").apply { if (!exists()) mkdirs() }

        // 2) Determinar siguiente índice tN (ej: t0, t1, ...)
        val nextIndex = tempsDir.listFiles()?.count { it.isDirectory } ?: 0
        val tDirName = "t$nextIndex"

        // 3) Crear subcarpetas tN en Temps e Imgs
        val tTemps = File(tempsDir, tDirName).apply { mkdirs() }
        val tImgs  = File(imgsDir,  tDirName).apply { mkdirs() }
        val tRegs        = File(regsDir,     tDirName).apply { mkdirs() }
        // NO creamos subdir en graphDir (quedará solo Grafica/)

        // 4) Guardar datos de temperaturas
        val jsonStr = intent.getStringExtra("tempsJson") ?: "{}"
        File(tTemps, "data.json").writeText(jsonStr)
        Log.d("PlotActivity", "Escrito JSON en: ${tTemps.absolutePath}/data.json")

        // 5) Guardar imagen ploteada
        //    Asumimos que en PlotActivity tienes la ruta de la imagen overlay o similares

        val imgPath = intent.getStringExtra("dermColoredPath")
            ?: intent.getStringExtra("dermColoredPath")
        imgPath?.let {
            val dest = File(tImgs, "image.png")
            File(it).copyTo(dest, overwrite = true)
            Log.d("PlotActivity", "Copiada imagen en: ${dest.absolutePath}")
        }
        val dermPath = intent.getStringExtra("dermContourPath")
            ?: intent.getStringExtra("dermContourPath")
        dermPath?.let {
            val dest = File(tRegs, "registro.png")
            File(it).copyTo(dest, overwrite = true)
            Log.d("PlotActivity", "Copiada imagen en: ${dest.absolutePath}")
        }

        // 6) Feedback al usuario
        Toast.makeText(this, "Guardado correctamente en $patientFolder/$tDirName", Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val REQUEST_NEW_PATIENT_SAVE = 2001
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_inicio       -> {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            R.id.nav_formulario   -> startActivity(Intent(this, FormularioActivity::class.java))
            R.id.nav_imagenes     -> { /* Ya estás aquí */ }
            R.id.nav_resultados   -> startActivity(Intent(this, ResultadosActivity::class.java))
            R.id.nav_basededatos  -> startActivity(Intent(this, BaseDeDatosActivity::class.java))
            R.id.nav_descargar_manual -> mostrarManualPDF()
        }
        findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawers()
        return true
    }

    private fun mostrarManualPDF() {
        // 1. Copiar el PDF de res/raw a cache
        val input = resources.openRawResource(R.raw.manual)
        val outFile = File(cacheDir, "manual.pdf")
        FileOutputStream(outFile).use { it.write(input.readBytes()) }

        // 2. Obtener URI protegido
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            outFile
        )

        // 3. Construir Intent de visualización mediante chooser
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val chooser = Intent.createChooser(viewIntent, "Abrir Manual PDF")

        // 4. Lanzar con manejo de excepción
        try {
            startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this,
                "No se encontró ninguna app para abrir PDF. Instala un lector de PDF.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(findViewById(R.id.nav_view))) {
            drawer.closeDrawers()
        } else {
            super.onBackPressed()
        }
    }
}
