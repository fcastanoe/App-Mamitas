package com.GCPDS.mamitas

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import com.GCPDS.mamitas.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    // ViewBinding
    private lateinit var binding: ActivityMainBinding

    // Toggle para el DrawerLayout ↔ hamburguesa
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflar el layout con ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar la Toolbar como ActionBar
        setSupportActionBar(binding.toolbar)

        // Inicializar y sincronizar el toggle (Drawer ↔ hamburguesa)
        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Asignar listener al NavigationView
        binding.navView.setNavigationItemSelectedListener(this)

        // Listeners de los botones del layout principal
        binding.btnFormulario.setOnClickListener {
            startActivity(Intent(this, FormularioActivity::class.java))
        }
        binding.btnImagenes.setOnClickListener {
            startActivity(Intent(this, MamitasAppActivity::class.java))
        }
        binding.btnResultados.setOnClickListener {
            startActivity(Intent(this, ResultadosActivity::class.java))
        }
        binding.btnBasededatos.setOnClickListener {
            startActivity(Intent(this, BaseDeDatosActivity::class.java))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Permite que el toggle controle la apertura/cierre del Drawer
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Manejo de selección de ítems del drawer
        when (item.itemId) {
            R.id.nav_inicio -> {
                // Si quisieras refrescar la misma Activity:
                // recreate()
            }
            R.id.nav_formulario -> startActivity(Intent(this, FormularioActivity::class.java))
            R.id.nav_imagenes   -> startActivity(Intent(this, MamitasAppActivity::class.java))
            R.id.nav_resultados  -> startActivity(Intent(this, ResultadosActivity::class.java))
            R.id.nav_basededatos -> startActivity(Intent(this, BaseDeDatosActivity::class.java))
        }
        binding.drawerLayout.closeDrawers()
        return true
    }

    override fun onBackPressed() {
        // Si el drawer está abierto, ciérralo en lugar de salir
        if (binding.drawerLayout.isDrawerOpen(binding.navView)) {
            binding.drawerLayout.closeDrawers()
        } else {
            super.onBackPressed()
        }
    }
}