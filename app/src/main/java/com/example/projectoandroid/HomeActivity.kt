package com.libros.projectoandroid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val logoutBtn = findViewById<Button>(R.id.logoutButton)
        val bibliotecaBtn = findViewById<Button>(R.id.bibliotecaButton)
        val agregarLibroBtn = findViewById<Button>(R.id.agregarLibroButton)

        bibliotecaBtn.setOnClickListener {
            startActivity(Intent(this, BibliotecaActivity::class.java))
        }

        agregarLibroBtn.setOnClickListener {
            startActivity(Intent(this, AgregarLibroActivity::class.java))
        }

        logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
