package com.example.projectoandroid

data class Libro(
    val id: String = "",
    val titulo: String = "",
    val autor: String = "",
    val genero: String = "",
    val idioma: String = "",
    val anio: String = "",
    val calificacion: Float = 0f,
    val pdfUrl: String = "",
    val portadaUrl: String = "" // URL de la imagen de portada
)