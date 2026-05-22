package com.example.projectoandroid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class HomeActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = Firebase.auth

        val logoutBtn = findViewById<Button>(R.id.logoutButton)
        val bibliotecaBtn = findViewById<Button>(R.id.bibliotecaButton)
        val agregarLibroBtn = findViewById<Button>(R.id.agregarLibroButton)

        bibliotecaBtn.setOnClickListener {
            val intent = Intent(this, BibliotecaActivity::class.java)
            startActivity(intent)
        }

        agregarLibroBtn.setOnClickListener {
            val intent = Intent(this, AgregarLibroActivity::class.java)
            startActivity(intent)
        }

        logoutBtn.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
