package com.libros.projectoandroid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class LibroAdapter(private var libros: List<Libro>) : RecyclerView.Adapter<LibroAdapter.LibroViewHolder>() {

    class LibroViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPortada: ImageView = view.findViewById(R.id.ivPortada)
        val tvTitulo: TextView = view.findViewById(R.id.tvTituloLibro)
        val rbLibro: RatingBar = view.findViewById(R.id.rbLibro)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibroViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_libro, parent, false)
        return LibroViewHolder(view)
    }

    override fun onBindViewHolder(holder: LibroViewHolder, position: Int) {
        val libro = libros[position]
        holder.tvTitulo.text = libro.titulo
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
    }

    override fun getItemCount() = libros.size

    fun updateLibros(nuevosLibros: List<Libro>) {
        libros = nuevosLibros
        notifyDataSetChanged()
    }
}
