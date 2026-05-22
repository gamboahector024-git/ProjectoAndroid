package com.example.projectoandroid

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
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

    private lateinit var storage: FirebaseStorage
    private lateinit var db: FirebaseFirestore

    private val selectPdfLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pdfUri = result.data?.data
            tvPdfStatus.text = "PDF seleccionado"
        }
    }

    private val selectPortadaLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            portadaUri = result.data?.data
            ivPortadaPreview.setImageURI(portadaUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_libro)

        storage = Firebase.storage
        db = Firebase.firestore

        // Inicializar vistas
        etTitulo = findViewById(R.id.tituloEditText)
        etAutor = findViewById(R.id.autorEditText)
        autoGenero = findViewById(R.id.generoAutoComplete)
        etAnio = findViewById(R.id.anioEditText)
        autoIdioma = findViewById(R.id.idiomaAutoComplete)
        rbCalificacion = findViewById(R.id.libroRatingBar)
        tvPdfStatus = findViewById(R.id.tvPdfStatus)
        ivPortadaPreview = findViewById(R.id.ivPortadaPreview)
        progressBar = findViewById(R.id.progressBar)
        
        val btnSeleccionarPdf = findViewById<Button>(R.id.btnSeleccionarPdf)
        val btnSeleccionarPortada = findViewById<Button>(R.id.btnSeleccionarPortada)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarLibro)

        // Configurar adaptadores
        val generos = arrayOf("Terror", "Fantasía", "Ciencia Ficción", "Misterio", "Romance", "Aventura", "Magia", "Poesía")
        autoGenero.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, generos))

        val idiomas = arrayOf("Español", "Inglés")
        autoIdioma.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, idiomas))

        btnSeleccionarPdf.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "application/pdf" }
            selectPdfLauncher.launch(intent)
        }

        btnSeleccionarPortada.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            selectPortadaLauncher.launch(intent)
        }

        btnGuardar.setOnClickListener {
            subirArchivosAFirebase()
        }
    }

    private fun subirArchivosAFirebase() {
        val titulo = etTitulo.text.toString().trim()
        val autor = etAutor.text.toString().trim()
        val genero = autoGenero.text.toString().trim()
        val idioma = autoIdioma.text.toString().trim()
        val anio = etAnio.text.toString().trim()
        val calificacion = rbCalificacion.rating

        if (titulo.isEmpty() || autor.isEmpty() || genero.isEmpty() || pdfUri == null || portadaUri == null) {
            Toast.makeText(this, "Por favor, completa todos los campos y selecciona ambos archivos", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnBloquearVistas(false)

        val storageRef = storage.reference
        val pdfName = "pdfs/${UUID.randomUUID()}.pdf"
        val imgName = "portadas/${UUID.randomUUID()}.jpg"

        val pdfRef = storageRef.child(pdfName)
        val imgRef = storageRef.child(imgName)

        // Subir PDF
        pdfRef.putFile(pdfUri!!).continueWithTask { task ->
            if (!task.isSuccessful) task.exception?.let { throw it }
            pdfRef.downloadUrl
        }.addOnCompleteListener { pdfTask ->
            if (pdfTask.isSuccessful) {
                val pdfUrl = pdfTask.result.toString()
                
                // Subir Portada
                imgRef.putFile(portadaUri!!).continueWithTask { task ->
                    if (!task.isSuccessful) task.exception?.let { throw it }
                    imgRef.downloadUrl
                }.addOnCompleteListener { imgTask ->
                    if (imgTask.isSuccessful) {
                        val imgUrl = imgTask.result.toString()
                        guardarEnFirestore(titulo, autor, genero, idioma, anio, calificacion, pdfUrl, imgUrl)
                    } else {
                        errorAlSubir("Error al subir la imagen")
                    }
                }
            } else {
                errorAlSubir("Error al subir el PDF")
            }
        }
    }

    private fun guardarEnFirestore(titulo: String, autor: String, genero: String, idioma: String, anio: String, calificacion: Float, pdfUrl: String, imgUrl: String) {
        val libro = Libro(
            id = UUID.randomUUID().toString(),
            titulo = titulo,
            autor = autor,
            genero = genero,
            idioma = idioma,
            anio = anio,
            calificacion = calificacion,
            pdfUrl = pdfUrl,
            portadaUrl = imgUrl
        )

        db.collection("libros").document(libro.id).set(libro)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Libro guardado exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                errorAlSubir("Error al guardar en la base de datos")
            }
    }

    private fun btnBloquearVistas(enabled: Boolean) {
        findViewById<Button>(R.id.btnGuardarLibro).isEnabled = enabled
    }

    private fun errorAlSubir(msg: String) {
        progressBar.visibility = View.GONE
        btnBloquearVistas(true)
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}