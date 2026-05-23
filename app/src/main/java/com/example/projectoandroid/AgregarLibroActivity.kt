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
        
        findViewById<Button>(R.id.btnSeleccionarPdf).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "application/pdf" }
            selectPdfLauncher.launch(intent)
        }

        findViewById<Button>(R.id.btnSeleccionarPortada).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            selectPortadaLauncher.launch(intent)
        }

        findViewById<Button>(R.id.btnGuardarLibro).setOnClickListener { subirArchivos() }

        val generos = arrayOf("Terror", "Fantasía", "Ciencia Ficción", "Misterio", "Romance", "Aventura")
        autoGenero.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, generos))
        
        val idiomas = arrayOf("Español", "Inglés")
        autoIdioma.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, idiomas))
    }

    private fun subirArchivos() {
        val titulo = etTitulo.text.toString().trim()
        if (titulo.isEmpty() || pdfUri == null || portadaUri == null) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        val storageRef = storage.reference
        val pdfRef = storageRef.child("libros/pdfs/${UUID.randomUUID()}.pdf")
        val imgRef = storageRef.child("libros/portadas/${UUID.randomUUID()}.jpg")

        pdfRef.putFile(pdfUri!!).addOnSuccessListener {
            pdfRef.downloadUrl.addOnSuccessListener { urlPdf ->
                imgRef.putFile(portadaUri!!).addOnSuccessListener {
                    imgRef.downloadUrl.addOnSuccessListener { urlImg ->
                        guardarLibro(urlPdf.toString(), urlImg.toString())
                    }
                }
            }
        }.addOnFailureListener {
            progressBar.visibility = View.GONE
            Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarLibro(pdfUrl: String, imgUrl: String) {
        val libro = Libro(
            id = UUID.randomUUID().toString(),
            titulo = etTitulo.text.toString(),
            autor = etAutor.text.toString(),
            genero = autoGenero.text.toString(),
            idioma = autoIdioma.text.toString(),
            anio = etAnio.text.toString(),
            calificacion = rbCalificacion.rating,
            pdfUrl = pdfUrl,
            portadaUrl = imgUrl
        )

        db.collection("libros").document(libro.id).set(libro)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Libro guardado", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
}
