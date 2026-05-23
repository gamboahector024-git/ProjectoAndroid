package com.libros.projectoandroid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class BibliotecaActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var rvLibros: RecyclerView
    private lateinit var adapter: LibroAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_biblioteca)

        rvLibros = findViewById(R.id.rvLibros)
        rvLibros.layoutManager = GridLayoutManager(this, 2)
        
        adapter = LibroAdapter(mutableListOf())
        rvLibros.adapter = adapter

        db.collection("libros").addSnapshotListener { value, _ ->
            val libros = value?.toObjects(Libro::class.java) ?: emptyList()
            adapter.updateLibros(libros)
        }
    }
}
