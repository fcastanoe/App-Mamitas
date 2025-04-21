package com.GCPDS.mamitas

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import com.bumptech.glide.Glide
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File
import java.io.FileOutputStream
import com.googlecode.tesseract.android.TessBaseAPI
import android.app.ProgressDialog
import android.graphics.Bitmap
import android.widget.ProgressBar
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.flex.FlexDelegate
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

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

class MamitasAppActivity : AppCompatActivity() {
    private lateinit var btnSelectImage: Button
    private lateinit var imgSelected: ImageView
    private lateinit var tvMaxTemp: TextView
    private lateinit var tvMinTemp: TextView
    private lateinit var btnModifyManual: Button
    private lateinit var btnStart: Button
    private lateinit var progressBar: ProgressBar

    // Launcher para seleccionar imagen
    private val getContent = registerForActivityResult(GetContent()) { uri: Uri? ->
        uri?.let {
            Glide.with(this).load(uri).into(imgSelected)
            processImage(uri)
        }
    }

    private lateinit var manualLauncher: ActivityResultLauncher<Intent>

    private lateinit var tvMessage: TextView  // <- nuevo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mamitas_app)

        // Vincular las vistas definidas en el layout
        btnSelectImage = findViewById(R.id.btnSelectImage)
        imgSelected = findViewById(R.id.imgSelected)
        tvMaxTemp         = findViewById(R.id.tvMaxTemp)
        tvMinTemp         = findViewById(R.id.tvMinTemp)
        tvMessage         = findViewById(R.id.tvMessage)
        btnModifyManual    = findViewById(R.id.btnModifyManual)
        btnStart           = findViewById(R.id.btnStart)
        progressBar = findViewById(R.id.progressBar)

        // Ocultamos todo al inicio
        listOf(tvMaxTemp, tvMinTemp, tvMessage, btnModifyManual, btnStart)
            .forEach { it.visibility = View.GONE }

        btnStart.setOnClickListener {
            // 1) Mostrar loader
            progressBar.visibility = View.VISIBLE

            Thread {
                try {
                    // a) Carga modelo con FlexDelegate si es necesario
                    val file = File(filesDir, "models/ResUNet_efficientnetb3_Mamitas.tflite")
                    val options = Interpreter.Options().addDelegate(FlexDelegate())
                    val interpreter = Interpreter(file, options)

                    // b) Preprocesado igual que en Python
                    val bitmap = BitmapFactory.decodeFile(lastImagePath)
                    val gray = bitmapToGray(bitmap)
                    val input = preprocessBitmap(gray, 512, 512) // adaptar dims

                    // c) Inferencia
                    val output = Array(1) { Array(512) { Array(512) { FloatArray(2) } } }
                    interpreter.run(input, output)

                    // d) Postprocesado y guardado de máscara
                    val maskBmp = outputToBitmap(output[0])
                    val maskFile = File(filesDir, "mask.png")
                    maskBmp.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(maskFile))

                    // 2) Ahora llamamos a Python sólo para plotear
                    val py = Python.getInstance()
                    val plot = py.getModule("plot")
                    val plotPath = plot.callAttr(
                        "run_plot", lastImagePath, filesDir.absolutePath
                    ).toString()

                    runOnUiThread {
                        // 2) Ocultar loader y abrir pantalla de plot
                        progressBar.visibility = View.GONE
                        // Abrir PlotActivity con rutas
                        startActivity(Intent(this, PlotActivity::class.java).apply {
                            putExtra("imagePath", lastImagePath)
                            putExtra("plotPath", plotPath)
                        })
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Error al segmentar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
        }

        // Registrar launcher para recibir datos editados
        manualLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { data ->
                    lastMaxTemp = data.getStringExtra("max_temp") ?: lastMaxTemp
                    lastMinTemp = data.getStringExtra("min_temp") ?: lastMinTemp
                    tvMaxTemp.text = "Max: ${lastMaxTemp}°C"
                    tvMinTemp.text = "Min: ${lastMinTemp}°C"
                }
            }
        }


        // Al presionar el botón se abre la galería para escoger una imagen
        btnSelectImage.setOnClickListener {
            getContent.launch("image/*")
        }
        btnModifyManual.setOnClickListener {
            // Lanzamos ResultActivity esperando un resultado
            Intent(this, ResultActivity::class.java).apply {
                putExtra("imagePath", lastImagePath)
                putExtra("max_temp", lastMaxTemp)
                putExtra("min_temp", lastMinTemp)
                manualLauncher.launch(this)
            }
        }

        // Inicializa Chaquopy si aún no se ha iniciado.
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        copyTessDataFiles(this)

    }

    // Variables para retener los últimos valores
    private var lastImagePath: String = ""
    private var lastMaxTemp: String = ""
    private var lastMinTemp: String = ""

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
        val imagePath = getPath(this, uri) ?: run {
            Toast.makeText(this, "Error al obtener la ruta de la imagen.", Toast.LENGTH_SHORT).show()
            return
        }
        copyTessDataFiles(this)

        lastImagePath = imagePath

        try {
            val (maxStr, minStr) = performOCROnImage(imagePath)
            lastMaxTemp = maxStr
            lastMinTemp = minStr

            val maxTemp = maxStr.toFloatOrNull()
            val minTemp = minStr.toFloatOrNull()

            // Caso: valores válidos y ambos en rango
            if (maxTemp != null && minTemp != null
                && maxTemp <= 40f && minTemp >= 15f) {
                showResults(maxStr, minStr)
                // nada más, dejamos los botones visibles como antes
                tvMessage.visibility = View.GONE
                btnStart.visibility = View.VISIBLE
                btnModifyManual.visibility = View.VISIBLE

            } else if (maxTemp != null && minTemp != null) {
                // Ambos detectados pero uno o ambos fuera de rango: mostramos mensaje
                showResults(maxStr, minStr)
                btnStart.visibility = View.VISIBLE
                btnModifyManual.visibility = View.VISIBLE
                tvMessage.visibility = View.VISIBLE

                // Construimos el texto según el caso
                tvMessage.text = when {
                    minTemp < 15f && maxTemp > 40f ->
                        "Mín <15°C y Máx >40°C. ¿Es correcto? Pulsa Start o Modificar."
                    minTemp < 15f ->
                        "Mínima <15°C. ¿Es correcta? Start o Modificar."
                    maxTemp > 40f ->
                        "Máxima >40°C. ¿Es correcta? Start o Modificar."
                    else -> ""
                }

            } else {
                // Falló OCR en alguno: vamos directo a edición manual
                Intent(this, ResultActivity::class.java).also { intent ->
                    intent.putExtra("imagePath", imagePath)
                    intent.putExtra("max_temp", maxStr)
                    intent.putExtra("min_temp", minStr)
                    startActivity(intent)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error en OCR: ${e.message}", Toast.LENGTH_LONG).show()
        }

    }

    private fun showResults(max: String, min: String) {
        tvMaxTemp.text = "Max: ${max}°C"
        tvMinTemp.text = "Min: ${min}°C"
        listOf(tvMaxTemp, tvMinTemp).forEach { it.visibility = View.VISIBLE }
    }
}


