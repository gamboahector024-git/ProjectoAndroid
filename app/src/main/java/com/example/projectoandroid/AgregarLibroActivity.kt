package com.libros.projectoandroid

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import coil.load
import java.util.UUID

class AgregarLibroActivity : AppCompatActivity() {

    private lateinit var etTitulo: TextInputEditText
    private lateinit var etAutor: TextInputEditText
    private lateinit var autoGenero: AutoCompleteTextView
    private lateinit var etAnio: TextInputEditText
    private lateinit var autoIdioma: AutoCompleteTextView
    private lateinit var rbCalificacion: RatingBar
    private lateinit var tvPdfStatus: TextView
    private lateinit var ivPortadaPreview: ImageView
    private lateinit var progressBar: ProgressBar

    private var pdfUri: Uri? = null
    private var portadaUri: Uri? = null
    
    private var libroId: String? = null
    private var existingPdfUrl: String? = null
    private var existingPortadaUrl: String? = null

    private val storage = FirebaseStorage.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val selectPdfLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pdfUri = result.data?.data
            tvPdfStatus.text = "PDF Seleccionado"
        }
    }

    private val selectPortadaLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            portadaUri = result.data?.data
            ivPortadaPreview.setImageURI(portadaUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_libro)

        etTitulo = findViewById(R.id.tituloEditText)
        etAutor = findViewById(R.id.autorEditText)
        autoGenero = findViewById(R.id.generoAutoComplete)
        etAnio = findViewById(R.id.anioEditText)
        autoIdioma = findViewById(R.id.idiomaAutoComplete)
        rbCalificacion = findViewById(R.id.libroRatingBar)
        tvPdfStatus = findViewById(R.id.tvPdfStatus)
        ivPortadaPreview = findViewById(R.id.ivPortadaPreview)
        progressBar = findViewById(R.id.progressBar)
        
        setupDropdowns()
        checkIntentExtras()

        findViewById<Button>(R.id.btnSeleccionarPdf).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "application/pdf" }
            selectPdfLauncher.launch(intent)
        }

        findViewById<Button>(R.id.btnSeleccionarPortada).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            selectPortadaLauncher.launch(intent)
        }

        findViewById<Button>(R.id.btnGuardarLibro).setOnClickListener { procesarGuardado() }
    }

    private fun setupDropdowns() {
        val generos = arrayOf("Terror", "Fantasía", "Ciencia Ficción", "Misterio", "Romance", "Aventura")
        autoGenero.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, generos))
        
        val idiomas = arrayOf("Español", "Inglés")
        autoIdioma.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, idiomas))
    }

    private fun checkIntentExtras() {
        libroId = intent.getStringExtra("EXTRA_ID")
        if (libroId != null) {
            // Modo Edición
            etTitulo.setText(intent.getStringExtra("EXTRA_TITULO"))
            etAutor.setText(intent.getStringExtra("EXTRA_AUTOR"))
            autoGenero.setText(intent.getStringExtra("EXTRA_GENERO"), false)
            etAnio.setText(intent.getStringExtra("EXTRA_ANIO"))
            autoIdioma.setText(intent.getStringExtra("EXTRA_IDIOMA"), false)
            rbCalificacion.rating = intent.getFloatExtra("EXTRA_CALIFICACION", 0f)
            
            existingPdfUrl = intent.getStringExtra("EXTRA_PDF_URL")
            existingPortadaUrl = intent.getStringExtra("EXTRA_PORTADA_URL")
            
            if (!existingPdfUrl.isNullOrEmpty()) tvPdfStatus.text = "PDF cargado (Click para cambiar)"
            if (!existingPortadaUrl.isNullOrEmpty()) ivPortadaPreview.load(existingPortadaUrl)
            
            findViewById<TextView>(android.R.id.title)?.text = "Editar Libro"
            findViewById<Button>(R.id.btnGuardarLibro).text = "Actualizar Libro"
        }
    }

    private fun procesarGuardado() {
        val titulo = etTitulo.text.toString().trim()
        if (titulo.isEmpty()) {
            Toast.makeText(this, "El título es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        if (libroId == null && (pdfUri == null || portadaUri == null)) {
            Toast.makeText(this, "Debes seleccionar PDF y Portada", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        
        if (pdfUri != null) {
            subirPdf()
        } else if (portadaUri != null) {
            subirPortada(existingPdfUrl ?: "")
        } else {
            guardarEnFirestore(existingPdfUrl ?: "", existingPortadaUrl ?: "")
        }
    }

    private fun subirPdf() {
        val pdfRef = storage.reference.child("libros/pdfs/${UUID.randomUUID()}.pdf")
        pdfRef.putFile(pdfUri!!).addOnSuccessListener {
            pdfRef.downloadUrl.addOnSuccessListener { urlPdf ->
                if (portadaUri != null) {
                    subirPortada(urlPdf.toString())
                } else {
                    guardarEnFirestore(urlPdf.toString(), existingPortadaUrl ?: "")
                }
            }
        }.addOnFailureListener {
            errorGuardado(it.message)
        }
    }

    private fun subirPortada(urlPdf: String) {
        val imgRef = storage.reference.child("libros/portadas/${UUID.randomUUID()}.jpg")
        imgRef.putFile(portadaUri!!).addOnSuccessListener {
            imgRef.downloadUrl.addOnSuccessListener { urlImg ->
                guardarEnFirestore(urlPdf, urlImg.toString())
            }
        }.addOnFailureListener {
            errorGuardado(it.message)
        }
    }

    private fun guardarEnFirestore(pdfUrl: String, imgUrl: String) {
        val id = libroId ?: UUID.randomUUID().toString()
        val libro = Libro(
            id = id,
            titulo = etTitulo.text.toString(),
            autor = etAutor.text.toString(),
            genero = autoGenero.text.toString(),
            idioma = autoIdioma.text.toString(),
            anio = etAnio.text.toString(),
            calificacion = rbCalificacion.rating,
            pdfUrl = pdfUrl,
            portadaUrl = imgUrl
        )

        db.collection("libros").document(id).set(libro)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, if (libroId == null) "Libro guardado" else "Libro actualizado", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                errorGuardado(it.message)
            }
    }

    private fun errorGuardado(error: String?) {
        progressBar.visibility = View.GONE
        Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
    }
}
