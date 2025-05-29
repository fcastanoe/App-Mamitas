package com.GCPDS.mamitas

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.chaquo.python.Python
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.chaquo.python.android.AndroidPlatform
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.load.resource.gif.GifDrawable
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestOptions
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import org.json.JSONObject
import java.io.File

class ResultadosActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var toggle: ActionBarDrawerToggle
    private val prefs by lazy { getSharedPreferences("app_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        setContentView(R.layout.activity_resultados)

        // Edge-to-edge padding
        val drawer = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
        ViewCompat.setOnApplyWindowInsetsListener(drawer) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        // Toolbar + drawer
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbarResultados)
        setSupportActionBar(toolbar)
        toggle = ActionBarDrawerToggle(
            this, drawer, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()
        findViewById<com.google.android.material.navigation.NavigationView>(R.id.nav_view)
            .setNavigationItemSelectedListener(this)

        // Vistas
        val btnSelect = findViewById<Button>(R.id.btnSelectPaciente)
        val ivGif      = findViewById<PhotoView>(R.id.ivGif)
        val chart      = findViewById<LineChart>(R.id.lineChart)
        val legendCont = findViewById<LinearLayout>(R.id.legendContainer)

        btnSelect.setOnClickListener {
            val patientDirs = prefs.getStringSet("patients", emptySet())!!
                .toMutableList().apply { add("Cancelar") }

            AlertDialog.Builder(this)
                .setTitle("Selecciona paciente")
                .setItems(patientDirs.toTypedArray()) { dialog, which ->
                    val choice = patientDirs[which]
                    if (choice != "Cancelar") {
                        dialog.dismiss()
                        // 1) anim GIF
                        generarYMostrarGif(choice, ivGif)
                        // 2) gráfica de temperatura
                        generarYMostrarChart(choice, chart, legendCont)
                    }
                }
                .show()
        }
    }

    private fun generarYMostrarGif(folderName: String, ivGif: PhotoView) {
        val py = Python.getInstance()
        val patientPath = File(filesDir, folderName).absolutePath
        val gifPath = py.getModule("gif")
            .callAttr("make_gif", patientPath)
            .toString()

        Glide.with(this)
            .asGif()
            .load(File(gifPath))
            .listener(object : RequestListener<GifDrawable> {
                override fun onResourceReady(
                    resource: GifDrawable, model: Any?,
                    target: Target<GifDrawable>?, dataSource: com.bumptech.glide.load.DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    resource.setLoopCount(1)
                    resource.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                        override fun onAnimationEnd(drawable: Drawable?) {
                            resource.stop()
                        }
                    })
                    return false
                }
                override fun onLoadFailed(
                    e: GlideException?, model: Any?, target: Target<GifDrawable>?, isFirstResource: Boolean
                ) = false
            })
            .into(ivGif)

        ivGif.setOnClickListener {
            // recarga para repetir
            Glide.with(this)
                .asGif()
                .load(File(gifPath))
                .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
                .into(ivGif)
        }
    }

    private fun generarYMostrarChart(
        folderName: String,
        chart: LineChart,
        legendCont: LinearLayout
    ) {
        // 1) leemos todas las tN de Temperaturas
        val base = File(filesDir, folderName)
        val tempsDir = File(base, "Temperaturas")
        val tFolders = tempsDir.listFiles { f -> f.isDirectory }
            ?.sortedBy { it.name.removePrefix("t").toInt() } ?: return

        // 2) construimos series
        val seriesMap = mutableMapOf<String, MutableList<Entry>>()
        tFolders.forEachIndexed { idx, dir ->
            val js = JSONObject(File(dir, "data.json").readText())
            js.keys().forEach { key ->
                val temp = js.getDouble(key).toFloat()
                seriesMap.getOrPut(key) { mutableListOf() }
                    .add(Entry(idx.toFloat(), temp))
            }
        }

        // 3) LineDataSets
        val labels = tFolders.map { it.name } // ["t0","t1",...]
        val COLORS = ChartActivity.COLORS
        val dataSets = seriesMap.entries.mapIndexed { i, (label, entries) ->
            LineDataSet(entries, label).apply {
                color     = COLORS[i % COLORS.size]
                lineWidth = 2f
                setDrawValues(false)
            }
        }

        // 4) configurar chart
        chart.data = LineData(dataSets)
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.isDragEnabled   = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)
        chart.legend.isWordWrapEnabled = true

        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            granularity    = 1f
            setLabelCount(labels.size, true)
        }

        chart.invalidate()
        chart.visibility = View.VISIBLE

        // 5) legend interactiva
        legendCont.removeAllViews()
        dataSets.forEach { set ->
            val cb = CheckBox(this).apply {
                text      = set.label
                isChecked = true
                setTextColor(set.color)
                setOnCheckedChangeListener { _, checked ->
                    set.isVisible = checked
                    chart.data.notifyDataChanged()
                    chart.notifyDataSetChanged()
                    chart.invalidate()
                }
            }
            legendCont.addView(cb)
        }
        (legendCont.parent as View).visibility = View.VISIBLE
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        if (toggle.onOptionsItemSelected(item)) true else super.onOptionsItemSelected(item)

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_inicio      -> startActivity(Intent(this, MainActivity::class.java))
            R.id.nav_formulario  -> startActivity(Intent(this, FormularioActivity::class.java))
            R.id.nav_imagenes    -> startActivity(Intent(this, MamitasAppActivity::class.java))
            R.id.nav_basededatos -> startActivity(Intent(this, BaseDeDatosActivity::class.java))
            // nav_resultados ya estás aquí
        }
        findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
            .closeDrawers()
        return true
    }
}