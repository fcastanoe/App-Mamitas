package com.GCPDS.mamitas

import android.graphics.Color
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
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import org.json.JSONObject


class ChartActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PATIENT_FOLDER = "extra_patient_folder"
        // Colores de material
        val COLORS = intArrayOf(
            Color.parseColor("#e6194B"), Color.parseColor("#3cb44b"), Color.parseColor("#ffe119"),
            Color.parseColor("#4363d8"), Color.parseColor("#f58231"), Color.parseColor("#911eb4"),
            Color.parseColor("#46f0f0"), Color.parseColor("#f032e6"), Color.parseColor("#bcf60c"),
            Color.parseColor("#fabebe")
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart)

        // 1) Listar subcarpetas t0, t1, …
        val base     = intent.getStringExtra(EXTRA_PATIENT_FOLDER)!!
        val tempsDir = File(base, "Temperaturas")
        val tFolders = tempsDir.listFiles { f -> f.isDirectory }
            ?.sortedBy { it.name.removePrefix("t").toInt() } ?: emptyList()

        // 2) Leer JSONs y agrupar
        val seriesMap = mutableMapOf<String, MutableList<Entry>>()
        tFolders.forEachIndexed { idx, dir ->
            val json = JSONObject(File(dir, "data.json").readText())
            json.keys().forEach { key ->
                val temp = json.getDouble(key).toFloat()
                seriesMap.getOrPut(key) { mutableListOf() }
                    .add(Entry(idx.toFloat(), temp))
            }
        }

        // 3) Crear DataSets con colores distintos
        val labels = tFolders.map { it.name }  // ["t0","t1",…]
        val dataSets = seriesMap.entries.mapIndexed { i, (label, entries) ->
            LineDataSet(entries, label).apply {
                color            = COLORS[i % COLORS.size]
                lineWidth        = 2f
                setDrawValues(false)
            }
        }

        // 4) Configurar LineChart
        val chart = findViewById<LineChart>(R.id.lineChart)
        chart.apply {
            data = LineData(dataSets)
            description.isEnabled      = false
            setTouchEnabled(true)
            isDragEnabled              = true
            setScaleEnabled(true)
            setPinchZoom(true)
            legend.isWordWrapEnabled   = true

            // Formatear eje X con ["t0","t1",…]
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity    = 1f
                setLabelCount(labels.size, true)
            }

            invalidate()
        }

        // 5) Leyenda interactiva con CheckBoxes
        val legendContainer = findViewById<LinearLayout>(R.id.legendContainer)
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
            legendContainer.addView(cb)
        }
    }
}