package com.GCPDS.mamitas

import android.os.Bundle
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import java.io.File
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import org.json.JSONObject


class ChartActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PATIENT_FOLDER = "extra_patient_folder"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart)

        // 1) Datos T0, T1, …
        val base       = intent.getStringExtra(EXTRA_PATIENT_FOLDER)!!
        val tempsDir   = File(base, "Temperaturas")
        val tFolders   = tempsDir.listFiles { f -> f.isDirectory }
            ?.sortedBy { it.name.removePrefix("t").toInt() } ?: emptyList()

        // 2) Leer JSONs y agrupar en seriesMap
        val seriesMap = mutableMapOf<String, MutableList<Entry>>()
        tFolders.forEachIndexed { idx, dir ->
            val json = JSONObject(File(dir, "data.json").readText())
            json.keys().forEach { key ->
                val temp = json.getDouble(key).toFloat()
                seriesMap.getOrPut(key) { mutableListOf() }
                    .add(Entry(idx.toFloat(), temp))
            }
        }

        // 3) Crear DataSets
        val dataSets = seriesMap.map { (label, entries) ->
            LineDataSet(entries, label).apply {
                lineWidth      = 2f
                setDrawValues(false)
            }
        }

        // 4) Configurar el gráfico
        val chart = findViewById<LineChart>(R.id.lineChart)
        chart.apply {
            data = LineData(dataSets)
            description.isEnabled      = false   // propiedad, OK
            setTouchEnabled(true)                // método
            isDragEnabled      = true            // propiedad
            setScaleEnabled(true)                // método
            setPinchZoom(true)                   // método
            legend.isWordWrapEnabled = true      // propiedad
            invalidate()                         // método
        }

        // 5) ¡Construir leyenda interactiva manual!
        val legendContainer = findViewById<LinearLayout>(R.id.legendContainer)
        dataSets.forEach { set ->
            val cb = CheckBox(this).apply {
                text       = set.label
                isChecked  = true
                setPadding(0, 8, 0, 8)
                setOnCheckedChangeListener { _, checked ->
                    // Oculta / muestra el DataSet y refresca
                    set.isVisible = checked
                    chart.data.notifyDataChanged()
                    chart.notifyDataSetChanged()
                    chart.invalidate()
                }
            }
            legendContainer.addView(cb)
        }
    }
}