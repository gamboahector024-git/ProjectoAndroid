package com.example.projectoandroid

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class BibliotecaActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var rvLibros: RecyclerView
    private lateinit var adapter: LibroAdapter
    private lateinit var filterGenero: AutoCompleteTextView
    private lateinit var filterIdioma: AutoCompleteTextView
    
    private var listaOriginal = mutableListOf<Libro>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_biblioteca)

        db = Firebase.firestore
        
        // Inicializar vistas
        rvLibros = findViewById(R.id.rvLibros)
        filterGenero = findViewById(R.id.filterGenero)
        filterIdioma = findViewById(R.id.filterIdioma)

        // Configurar RecyclerView con un grid de 2 columnas
        rvLibros.layoutManager = GridLayoutManager(this, 2)
        adapter = LibroAdapter(emptyList())
        rvLibros.adapter = adapter

        setupFilters()
        fetchLibros()
    }

    private fun setupFilters() {
        val generos = arrayOf("Todos", "Terror", "Fantasía", "Ciencia Ficción", "Misterio", "Romance", "Aventura", "Magia", "Poesía")
        val idiomas = arrayOf("Todos", "Español", "Inglés")

        filterGenero.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, generos))
        filterIdioma.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, idiomas))

        filterGenero.setOnItemClickListener { _, _, _, _ -> applyFilters() }
        filterIdioma.setOnItemClickListener { _, _, _, _ -> applyFilters() }
    }

    private fun fetchLibros() {
        db.collection("libros")
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener
                
                listaOriginal = value?.toObjects(Libro::class.java) ?: mutableListOf()
                applyFilters()
            }
    }

    private fun applyFilters() {
        val generoSeleccionado = filterGenero.text.toString()
        val idiomaSeleccionado = filterIdioma.text.toString()

        val listaFiltrada = listaOriginal.filter { libro ->
            val matchGenero = generoSeleccionado == "Todos" || generoSeleccionado.isEmpty() || libro.genero == generoSeleccionado
            val matchIdioma = idiomaSeleccionado == "Todos" || idiomaSeleccionado.isEmpty() || libro.idioma == idiomaSeleccionado
            matchGenero && matchIdioma
        }

        adapter.updateLibros(listaFiltrada)
    }
}