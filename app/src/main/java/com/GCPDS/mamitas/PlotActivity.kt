package com.GCPDS.mamitas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import org.json.JSONObject
import java.io.FileOutputStream

class PlotActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var toggle: ActionBarDrawerToggle

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

        intent.getStringExtra("dermContourPath")?.let { path ->
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
                    setPadding(0,8,0,8)
                }
                tempsCont.addView(tv)
            }
        }

        // 3) Botón Guardar → FormularioActivity
        btnSave.setOnClickListener {
            startActivity(Intent(this, FormularioActivity::class.java))
        }

        // 4) Botón Nueva imagen (igual que antes)
        btnReset.setOnClickListener {
            finish()  // cierra PlotActivity para seleccionar otra
        }
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
        }
        findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawers()
        return true
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
