package com.GCPDS.mamitas

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView

class BaseDeDatosActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_de_datos)

        // 1) Toolbar + Drawer
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val drawer = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        toggle = ActionBarDrawerToggle(
            this, drawer, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()
        navView.setNavigationItemSelectedListener(this)

        // 2) Listar “assets/Database” y ordenar naturalmente
        val assetMgr = assets
        val rawCases = assetMgr.list("Database")?.toList() ?: emptyList()
        val cases = rawCases.sortedWith(compareBy { name ->
            // extrae el número tras "Caso_" y lo convierte a Int
            name.substringAfterLast(' ').toIntOrNull() ?: Int.MAX_VALUE
        })

        // 3) RecyclerView con AssetAdapter
        val rvCases = findViewById<RecyclerView>(R.id.rvCases)
        rvCases.layoutManager = LinearLayoutManager(this)
        rvCases.adapter = AssetAdapter(
            items     = cases,
            assetBase = "Database"
        ) { name ->
            // al pulsar “Caso_X” (siempre carpeta), abrimos AssetBrowserActivity
            startActivity(Intent(this, AssetBrowserActivity::class.java).apply {
                putExtra(AssetBrowserActivity.EXTRA_ASSET_PATH, "Database/$name")
            })
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_inicio      -> startActivity(Intent(this, MainActivity::class.java))
            R.id.nav_formulario  -> startActivity(Intent(this, FormularioActivity::class.java))
            R.id.nav_imagenes    -> startActivity(Intent(this, MamitasAppActivity::class.java))
            R.id.nav_resultados  -> startActivity(Intent(this, ResultadosActivity::class.java))
            R.id.nav_basededatos -> { /* Ya estás aquí */ }
        }
        findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
            .closeDrawers()
        return true
    }

    override fun onBackPressed() {
        val drawer = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(findViewById(R.id.nav_view))) {
            drawer.closeDrawers()
        } else {
            super.onBackPressed()
        }
    }
}