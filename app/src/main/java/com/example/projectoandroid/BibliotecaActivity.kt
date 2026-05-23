package com.libros.projectoandroid

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class BibliotecaActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var rvLibros: RecyclerView
    private lateinit var adapter: LibroAdapter
    
    private var listaCompletaLibros: List<Libro> = emptyList()

    private lateinit var filterGenero: AutoCompleteTextView
    private lateinit var filterIdioma: AutoCompleteTextView
    private lateinit var filterAnio: TextInputEditText
    private lateinit var filterCalificacion: AutoCompleteTextView
    private lateinit var btnClearFilters: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_biblioteca)

        rvLibros = findViewById(R.id.rvLibros)
        rvLibros.layoutManager = GridLayoutManager(this, 2)
        
        adapter = LibroAdapter(
            libros = mutableListOf(),
            onItemClick = { libro -> abrirPdf(libro.pdfUrl) },
            onItemLongClick = { libro -> mostrarDetallesLibro(libro) }
        )
        rvLibros.adapter = adapter

        setupFilters()

        db.collection("libros").addSnapshotListener { value, error ->
            if (error != null) {
                Toast.makeText(this, "Error al cargar libros", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            listaCompletaLibros = value?.toObjects(Libro::class.java) ?: emptyList()
            aplicarFiltros()
        }
    }

    private fun setupFilters() {
        filterGenero = findViewById(R.id.filterGenero)
        filterIdioma = findViewById(R.id.filterIdioma)
        filterAnio = findViewById(R.id.filterAnio)
        filterCalificacion = findViewById(R.id.filterCalificacion)
        btnClearFilters = findViewById(R.id.btnClearFilters)

        val generos = arrayOf("Terror", "Fantasía", "Ciencia Ficción", "Misterio", "Romance", "Aventura")
        val idiomas = arrayOf("Español", "Inglés")
        val calificaciones = arrayOf("1", "2", "3", "4", "5")
        
        filterGenero.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, generos))
        filterIdioma.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, idiomas))
        filterCalificacion.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, calificaciones))
        
        filterGenero.setOnItemClickListener { _, _, _, _ -> aplicarFiltros() }
        filterIdioma.setOnItemClickListener { _, _, _, _ -> aplicarFiltros() }
        filterCalificacion.setOnItemClickListener { _, _, _, _ -> aplicarFiltros() }
        
        // Listener para que el filtro de año funcione mientras escribes
        filterAnio.doOnTextChanged { _, _, _, _ -> aplicarFiltros() }

        btnClearFilters.setOnClickListener {
            filterGenero.setText("", false)
            filterIdioma.setText("", false)
            filterAnio.setText("")
            filterCalificacion.setText("", false)
            aplicarFiltros()
        }
    }

    private fun aplicarFiltros() {
        val genero = filterGenero.text.toString()
        val idioma = filterIdioma.text.toString()
        val anio = filterAnio.text.toString()
        val califStr = filterCalificacion.text.toString()
        val califMin = if (califStr.isNotEmpty()) califStr.toFloat() else 0f

        val librosFiltrados = listaCompletaLibros.filter { libro ->
            (genero.isEmpty() || libro.genero == genero) &&
            (idioma.isEmpty() || libro.idioma == idioma) &&
            (anio.isEmpty() || libro.anio.contains(anio)) &&
            (libro.calificacion >= califMin)
        }

        adapter.updateLibros(librosFiltrados)
    }

    private fun mostrarDetallesLibro(libro: Libro) {
        val detalles = """
            Título: ${libro.titulo}
            Autor: ${libro.autor}
            Género: ${libro.genero}
            Año: ${libro.anio}
            Idioma: ${libro.idioma}
            Calificación: ${libro.calificacion} / 5.0
        """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("Información del Libro")
            .setMessage(detalles)
            .setPositiveButton("Leer") { _, _ -> abrirPdf(libro.pdfUrl) }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun abrirPdf(url: String) {
        if (url.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(url), "application/pdf")
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            
            val chooser = Intent.createChooser(intent, "Abrir PDF con...")
            try {
                startActivity(chooser)
            } catch (e: Exception) {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            }
        } else {
            Toast.makeText(this, "El libro no tiene un PDF asociado", Toast.LENGTH_SHORT).show()
        }
    }
}
