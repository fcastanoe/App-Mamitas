package com.GCPDS.mamitas

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.bumptech.glide.Glide
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.chaquo.python.PyObject
import java.io.File
import java.io.FileOutputStream
import com.googlecode.tesseract.android.TessBaseAPI
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.flex.FlexDelegate
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import com.GCPDS.mamitas.databinding.ActivityMamitasAppBinding
import com.google.android.material.navigation.NavigationView

// 1) Convierte un Bitmap a escala de grises.
//    Mantiene el mismo tamaño que el bitmap original.
fun bitmapToGray(input: Bitmap): Bitmap {
    val width = input.width
    val height = input.height
    val grayBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(grayBmp)
    val paint = Paint()
    val cm = android.graphics.ColorMatrix().apply {
        setSaturation(0f)
    }
    paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
    canvas.drawBitmap(input, 0f, 0f, paint)
    return grayBmp
}

// 2) Preprocesa el Bitmap en un array compatible con Interpreter.run()
//    Crea un tensor [1, targetH, targetW, 3] de floats normalizados [0..1].
fun preprocessBitmap(grayBmp: Bitmap, targetW: Int, targetH: Int): Array<Array<Array<FloatArray>>> {
    // Redimensiona el grayscale a (targetW, targetH)
    val resized = Bitmap.createScaledBitmap(grayBmp, targetW, targetH, true)
    // Construye el array [1][H][W][3]
    val input = Array(1) {
        Array(targetH) { y ->
            Array(targetW) { x ->
                // Lee el pixel (gris) y convierte a float RGB igual
                val c = resized.getPixel(x, y) and 0xFF
                val v = c / 255f
                FloatArray(3) { v }  // [v, v, v]
            }
        }
    }
    return input
}

// 3) Convierte la salida del modelo (Array[H][W][2]) a un Bitmap de máscara
//    Asume que la última dimensión es el canal de clases;
//    marca en blanco las celdas con probabilidad clase 1 > 0.5, negro el resto.
fun outputToBitmap(output: Array<Array<FloatArray>>): Bitmap {
    val height = output.size
    val width = output[0].size
    val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val probs = output[y][x]  // FloatArray de longitud 2
            // Usa umbral 0.5 en el canal 1
            val v = if (probs[1] > 0.5f) 255 else 0
            mask.setPixel(x, y, Color.argb(255, v, v, v))
        }
    }
    return mask
}

fun copyTessDataFiles(context: Context) {
    val assetManager = context.assets
    val tessdataDir = File(context.filesDir, "tesseract/tessdata")
    if (!tessdataDir.exists()) {
        tessdataDir.mkdirs()
    }
    val fileList = assetManager.list("tessdata")
    fileList?.forEach { filename ->
        val outFile = File(tessdataDir, filename)
        if (!outFile.exists()) {
            assetManager.open("tessdata/$filename").use { inputStream ->
                FileOutputStream(outFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }
}

// Funciones de ayuda para obtener la ruta real del archivo a partir de un URI.
fun getPath(context: Context, uri: Uri): String? {
    // Verifica si el URI es de tipo DocumentProvider (Android KitKat+)
    if (DocumentsContract.isDocumentUri(context, uri)) {
        val docId = DocumentsContract.getDocumentId(uri)
        val split = docId.split(":")
        val type = split[0]
        // Si es imagen, usa MediaStore
        if ("image" == type) {
            val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val selection = "_id=?"
            val selectionArgs = arrayOf(split[1])
            return getDataColumn(context, contentUri, selection, selectionArgs)
        }
        // Puedes expandir para otros tipos si es necesario.
    } else if ("content".equals(uri.scheme, ignoreCase = true)) {
        return getDataColumn(context, uri, null, null)
    } else if ("file".equals(uri.scheme, ignoreCase = true)) {
        return uri.path
    }
    return null
}

private fun getDataColumn(
    context: Context,
    uri: Uri,
    selection: String?,
    selectionArgs: Array<String>?
): String? {
    var cursor: Cursor? = null
    val column = MediaStore.Images.Media.DATA
    val projection = arrayOf(column)
    try {
        cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
        if (cursor != null && cursor.moveToFirst()) {
            val column_index = cursor.getColumnIndexOrThrow(column)
            return cursor.getString(column_index)
        }
    } finally {
        cursor?.close()
    }
    return null
}



class MamitasAppActivity: AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMamitasAppBinding
    private lateinit var toggle: ActionBarDrawerToggle

    // Launcher para seleccionar imagen
    private val getContent = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Glide.with(this).load(it).into(binding.imgSelected)
            processImage(it)
        }
    }

    private lateinit var manualLauncher: ActivityResultLauncher<Intent>

    // Variables para retener los últimos valores
    private var lastImagePath: String = ""
    private var lastMaxTemp: String = ""
    private var lastMinTemp: String = ""



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1) Inflamos con ViewBinding y establecemos el layout
        binding = ActivityMamitasAppBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2) Toolbar + Drawer
        setSupportActionBar(binding.toolbar)
        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        binding.navView.setNavigationItemSelectedListener(this)



        // 3) Ocultamos al inicio las vistas secundarias
        listOf(
            binding.tvMaxTemp,
            binding.tvMinTemp,
            binding.tvMessage,
            binding.btnModifyManual,
            binding.btnStart,
            binding.progressBar
        ).forEach { it.visibility = View.GONE }

        // 2) Usa el launcher en el listener del botón:
        binding.btnSelectImage.setOnClickListener {
            // Pasamos el MIME type "image/*" para filtrar solo imágenes
            getContent.launch("image/*")
        }

        // 5) "Start" para ejecutar la inferencia
        binding.btnStart.setOnClickListener {
            runInference()
        }

        // 6) "Modificar manualmente" abre ResultActivity para editar
        binding.btnModifyManual.setOnClickListener {
            Intent(this, ResultActivity::class.java).also { intent ->
                intent.putExtra("imagePath", lastImagePath)
                intent.putExtra("max_temp", lastMaxTemp)
                intent.putExtra("min_temp", lastMinTemp)
                manualLauncher.launch(intent)
            }
        }

        // Registrar launcher para recibir datos editados
        manualLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { data ->
                    lastMaxTemp = data.getStringExtra("max_temp") ?: lastMaxTemp
                    lastMinTemp = data.getStringExtra("min_temp") ?: lastMinTemp
                    binding.tvMaxTemp.text = "Max: ${lastMaxTemp}°C"
                    binding.tvMinTemp.text = "Min: ${lastMinTemp}°C"
                }
            }
        }


        // Inicializa Chaquopy si aún no se ha iniciado.
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        copyTessDataFiles(this)

    }

    /** Ejecuta la inferencia TensorFlow Lite en background */
    private fun runInference() {
        binding.progressBar.visibility = View.VISIBLE

        Thread {
            try {
                // Cargo modelo
                val modelFile = File(filesDir, "models/ResUNet_efficientnetb3_Mamitas.tflite")
                val options = Interpreter.Options().addDelegate(FlexDelegate())
                val interpreter = Interpreter(modelFile, options)

                // Preparo bitmap
                val bmp = BitmapFactory.decodeFile(lastImagePath)
                val gray = bitmapToGray(bmp)
                val input = preprocessBitmap(gray, 512, 512)

                // Ejecuto
                val output = Array(1) { Array(512) { Array(512) { FloatArray(2) } } }
                interpreter.run(input, output)

                // Post–process
                val maskBmp = outputToBitmap(output[0])
                File(filesDir, "mask.png").outputStream().use { out ->
                    maskBmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                // Llamada a Python para plots
                val py = Python.getInstance()
                val mod: PyObject = py.getModule("plot")
                val results = mod.callAttr(
                    "run_plot",
                    lastImagePath,
                    filesDir.absolutePath,
                    lastMaxTemp,
                    lastMinTemp
                ).asList()

                val segOverlay = results[1].toString()
                val dermContours = results[2].toString()
                val tempsJson = results[3].toString()

                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    startActivity(Intent(this, PlotActivity::class.java).apply {
                        putExtra("segOverlayPath", segOverlay)
                        putExtra("dermContourPath", dermContours)
                        putExtra("tempsJson", tempsJson)
                    })
                }
            } catch (e: Exception) {
                Log.e("InferenceError", e.message ?: "Error en inferencia")
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Error en inferencia: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_inicio       -> { // Lanzar MainActivity y cerrar la actual para evitar back-stack redundante
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            R.id.nav_formulario   -> startActivity(Intent(this, FormularioActivity::class.java))
            R.id.nav_imagenes     -> { /* Ya estás aquí */ }
            R.id.nav_resultados   -> startActivity(Intent(this, ResultadosActivity::class.java))
            R.id.nav_basededatos  -> startActivity(Intent(this, BaseDeDatosActivity::class.java))
        }
        binding.drawerLayout.closeDrawers()
        return true
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(binding.navView)) {
            binding.drawerLayout.closeDrawers()
        } else {
            super.onBackPressed()
        }
    }


    fun performOCROnImage(imagePath: String): Pair<String, String> {
        // Crea una instancia de TessBaseAPI
        val tessBaseAPI = TessBaseAPI()

        // Define la ruta para los datos de Tesseract.
        // Supongamos que has copiado los datos en getFilesDir()/tesseract/
        val dataPath = File(filesDir, "tesseract").absolutePath

        // Inicializa para el idioma que necesites (por ejemplo, "eng")
        if (!tessBaseAPI.init(dataPath, "eng")) {
            tessBaseAPI.end()
            throw Exception("Error al iniciar Tesseract")
        }

        // Carga la imagen (puedes usar BitmapFactory para abrir la imagen)
        val bitmap = BitmapFactory.decodeFile(imagePath)
        if (bitmap == null) {
            tessBaseAPI.end()
            throw Exception("Error al cargar la imagen")
        }

        tessBaseAPI.setImage(bitmap)
        // Obtiene el texto reconocido
        val recognizedText = tessBaseAPI.utF8Text ?: ""
        tessBaseAPI.end()

        // Suponiendo que en tu imagen están ambas temperaturas, puedes utilizar alguna expresión regular para extraer los valores.
        // Por simplicidad, si el texto tiene dos números, el primero lo tomamos como temperatura máxima y el segundo como mínima.
        // Esto debes ajustarlo según el formato de tu imagen.
        val regex = Regex("""\d+(?:\.\d+)?""")
        val matches = regex.findAll(recognizedText).map { it.value }.toList()
        val maxTemp = if (matches.isNotEmpty()) matches[0] else ""
        val minTemp = if (matches.size >= 2) matches[1] else ""

        return Pair(maxTemp, minTemp)
    }
    // Función para llamar al script Python que procesa la imagen.
    private fun processImage(uri: Uri) {
        val path = getPath(this, uri)
        if (path == null) {
            Toast.makeText(this, "No se pudo obtener ruta", Toast.LENGTH_SHORT).show()
            return
        }
        lastImagePath = path

        try {
            val (maxS, minS) = performOCROnImage(path)
            lastMaxTemp = maxS
            lastMinTemp = minS

            binding.tvMaxTemp.text = "Max: ${maxS}°C"
            binding.tvMinTemp.text = "Min: ${minS}°C"
            binding.tvMaxTemp.visibility = View.VISIBLE
            binding.tvMinTemp.visibility = View.VISIBLE

            val max = maxS.toFloatOrNull()
            val min = minS.toFloatOrNull()

            when {
                max == null || min == null -> {
                    // Saltar directo a edición
                    Intent(this, ResultActivity::class.java).also { it.putExtra("imagePath", path) }
                        .let { startActivity(it) }
                }
                max in 15f..40f && min in 15f..40f -> {
                    binding.tvMessage.visibility = View.GONE
                    binding.btnStart.visibility = View.VISIBLE
                    binding.btnModifyManual.visibility = View.VISIBLE
                }
                else -> {
                    binding.tvMessage.visibility = View.VISIBLE
                    binding.btnStart.visibility = View.VISIBLE
                    binding.btnModifyManual.visibility = View.VISIBLE
                    binding.tvMessage.text = buildString {
                        if (min < 15f) append("Mín <15°C. ")
                        if (max > 40f) append("Máx >40°C. ")
                        append("¿Es correcto? Pulsa Start o Modificar.")
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error OCR: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}


