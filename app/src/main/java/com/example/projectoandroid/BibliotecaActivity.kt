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
            onEditClick = { libro -> editarLibro(libro) },
            onDeleteClick = { libro -> confirmarEliminacion(libro) }
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

    private fun editarLibro(libro: Libro) {
        val intent = Intent(this, AgregarLibroActivity::class.java).apply {
            putExtra("EXTRA_ID", libro.id)
            putExtra("EXTRA_TITULO", libro.titulo)
            putExtra("EXTRA_AUTOR", libro.autor)
            putExtra("EXTRA_GENERO", libro.genero)
            putExtra("EXTRA_ANIO", libro.anio)
            putExtra("EXTRA_IDIOMA", libro.idioma)
            putExtra("EXTRA_CALIFICACION", libro.calificacion)
            putExtra("EXTRA_PDF_URL", libro.pdfUrl)
            putExtra("EXTRA_PORTADA_URL", libro.portadaUrl)
        }
        startActivity(intent)
    }

    private fun confirmarEliminacion(libro: Libro) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar Libro")
            .setMessage("¿Estás seguro de que deseas eliminar '${libro.titulo}'? Esta acción no se puede deshacer.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                db.collection("libros").document(libro.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Libro eliminado", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show()
                    }
            }
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
