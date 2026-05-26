package com.libros.projectoandroid

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class BibliotecaActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var rvLibros: RecyclerView
    private lateinit var adapter: LibroAdapter
    
    private var listaCompletaLibros: List<Libro> = emptyList()

    // Componentes del Menú Lateral
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnMenu: MaterialButton
    private lateinit var drawerSearchText: TextInputEditText
    private lateinit var drawerAuthorText: TextInputEditText
    private lateinit var drawerAnioText: TextInputEditText
    private lateinit var drawerChipIdioma: ChipGroup
    private lateinit var drawerGenero: AutoCompleteTextView
    private lateinit var drawerRatingBar: RatingBar
    private lateinit var btnAplicarFiltros: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_biblioteca)

        drawerLayout = findViewById(R.id.drawerLayout)
        btnMenu = findViewById(R.id.btnMenu)
        
        rvLibros = findViewById(R.id.rvLibros)
        rvLibros.layoutManager = GridLayoutManager(this, 2)
        
        adapter = LibroAdapter(
            libros = mutableListOf(),
            onItemClick = { libro -> abrirPdf(libro.pdfUrl) },
            onEditClick = { libro -> editarLibro(libro) },
            onDeleteClick = { libro -> confirmarEliminacion(libro) }
        )
        rvLibros.adapter = adapter

        setupDrawerMenu()

        db.collection("libros").addSnapshotListener { value, error ->
            if (error != null) {
                Toast.makeText(this, "Error al cargar libros", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            listaCompletaLibros = value?.toObjects(Libro::class.java) ?: emptyList()
            aplicarFiltros()
        }

        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupDrawerMenu() {
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        // Como drawer_filters está incluido directamente en el NavigationView (no como header)
        // podemos buscar los IDs directamente en el navigationView
        
        drawerSearchText = navigationView.findViewById(R.id.drawerSearchText)
        drawerAuthorText = navigationView.findViewById(R.id.drawerAuthorText)
        drawerAnioText = navigationView.findViewById(R.id.drawerAnioText)
        drawerChipIdioma = navigationView.findViewById(R.id.drawerChipIdioma)
        drawerGenero = navigationView.findViewById(R.id.drawerGenero)
        drawerRatingBar = navigationView.findViewById(R.id.drawerRatingBar)
        btnAplicarFiltros = navigationView.findViewById(R.id.btnAplicarFiltros)

        val generos = arrayOf("Todos", "Terror", "Fantasía", "Ciencia Ficción", "Misterio", "Romance", "Aventura")
        drawerGenero.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, generos))
        drawerGenero.setText("Todos", false)

        btnAplicarFiltros.setOnClickListener {
            aplicarFiltros()
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun aplicarFiltros() {
        val tituloBusqueda = if (::drawerSearchText.isInitialized) drawerSearchText.text.toString().trim() else ""
        val autorBusqueda = if (::drawerAuthorText.isInitialized) drawerAuthorText.text.toString().trim() else ""
        val anioBusqueda = if (::drawerAnioText.isInitialized) drawerAnioText.text.toString().trim() else ""
        val generoDrawer = if (::drawerGenero.isInitialized) drawerGenero.text.toString().trim() else ""
        val ratingMinimo = if (::drawerRatingBar.isInitialized) drawerRatingBar.rating else 0f
        
        val selectedChipId = if (::drawerChipIdioma.isInitialized) drawerChipIdioma.checkedChipId else View.NO_ID
        val idiomaDrawer = if (selectedChipId != View.NO_ID) {
            drawerChipIdioma.findViewById<Chip>(selectedChipId).text.toString()
        } else "Todos"

        val librosFiltrados = listaCompletaLibros.filter { libro ->
            val matchTitulo = tituloBusqueda.isEmpty() || libro.titulo.contains(tituloBusqueda, ignoreCase = true)
            val matchAutor = autorBusqueda.isEmpty() || libro.autor.contains(autorBusqueda, ignoreCase = true)
            val matchAnio = anioBusqueda.isEmpty() || libro.anio.trim() == anioBusqueda
            val matchGenero = generoDrawer.isEmpty() || generoDrawer == "Todos" || libro.genero.equals(generoDrawer, ignoreCase = true)
            val matchIdioma = idiomaDrawer == "Todos" || libro.idioma.equals(idiomaDrawer, ignoreCase = true)
            val matchRating = libro.calificacion >= ratingMinimo

            matchTitulo && matchAutor && matchAnio && matchGenero && matchIdioma && matchRating
        }

        adapter.updateLibros(librosFiltrados)
        
        if (librosFiltrados.isEmpty() && listaCompletaLibros.isNotEmpty()) {
            Toast.makeText(this, "No se encontraron libros con esos filtros", Toast.LENGTH_SHORT).show()
        }
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
            .setMessage("¿Estás seguro de que deseas eliminar '${libro.titulo}'?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                db.collection("libros").document(libro.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Libro eliminado", Toast.LENGTH_SHORT).show()
                    }
            }
            .show()
    }

    private fun abrirPdf(url: String) {
        if (url.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(url), "application/pdf")
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(Intent.createChooser(intent, "Abrir PDF"))
        } else {
            Toast.makeText(this, "Sin PDF", Toast.LENGTH_SHORT).show()
        }
    }
}
