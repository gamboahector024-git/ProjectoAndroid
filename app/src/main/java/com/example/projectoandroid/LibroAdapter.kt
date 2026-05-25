package com.libros.projectoandroid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.button.MaterialButton

class LibroAdapter(
    private var libros: List<Libro>,
    private val onItemClick: (Libro) -> Unit,
    private val onEditClick: (Libro) -> Unit,
    private val onDeleteClick: (Libro) -> Unit
) : RecyclerView.Adapter<LibroAdapter.LibroViewHolder>() {

    class LibroViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPortada: ImageView = view.findViewById(R.id.ivPortada)
        val tvTitulo: TextView = view.findViewById(R.id.tvTituloLibro)
        val tvAutor: TextView = view.findViewById(R.id.tvAutorLibro)
        val rbLibro: RatingBar = view.findViewById(R.id.rbLibro)
        val btnVer: MaterialButton = view.findViewById(R.id.btnVerLibro)
        val btnEditar: ImageButton = view.findViewById(R.id.btnEditarLibro)
        val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminarLibro)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibroViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_libro, parent, false)
        return LibroViewHolder(view)
    }

    override fun onBindViewHolder(holder: LibroViewHolder, position: Int) {
        val libro = libros[position]
        holder.tvTitulo.text = libro.titulo
        holder.tvAutor.text = libro.autor
        holder.rbLibro.rating = libro.calificacion
        
        if (libro.portadaUrl.isNotEmpty()) {
            holder.ivPortada.load(libro.portadaUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_report_image)
                error(android.R.drawable.ic_menu_report_image)
            }
        } else {
            holder.ivPortada.setImageResource(android.R.drawable.ic_menu_agenda)
        }

        holder.btnVer.setOnClickListener { onItemClick(libro) }
        holder.btnEditar.setOnClickListener { onEditClick(libro) }
        holder.btnEliminar.setOnClickListener { onDeleteClick(libro) }
        
        holder.itemView.setOnClickListener { onItemClick(libro) }
    }

    override fun getItemCount() = libros.size

    fun updateLibros(nuevosLibros: List<Libro>) {
        libros = nuevosLibros
        notifyDataSetChanged()
    }
}
