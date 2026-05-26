package com.libros.projectoandroid

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var progressBar: ProgressBar

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        showLoading(false)
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                showLoading(true)
                firebaseAuthWithGoogle(idToken)
            } else {
                Toast.makeText(this, "No se pudo obtener el token de Google", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.e("GoogleSignIn", "Error code: ${e.statusCode}, message: ${e.message}")
            val msg = when (e.statusCode) {
                7 -> "Error de red. Verifica tu conexión."
                10 -> "Error de configuración (Developer Error). Verifica el SHA-1 en Firebase."
                12500 -> "Error de actualización de Google Play Services."
                else -> "Error de Google (${e.statusCode})"
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = Firebase.auth
        progressBar = findViewById(R.id.progressBar)

        // Usamos R.string.default_web_client_id generado por el plugin de Google Services
        // para asegurar que siempre use el ID correcto configurado en google-services.json
        val webClientId = getString(R.string.default_web_client_id)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setupButtons()
    }

    private fun setupButtons() {
        val emailET = findViewById<EditText>(R.id.emailEditText)
        val passwordET = findViewById<EditText>(R.id.passwordEditText)
        val loginBtn = findViewById<Button>(R.id.loginButton)
        val registerBtn = findViewById<Button>(R.id.registerButton)
        val googleBtn = findViewById<SignInButton>(R.id.googleSignInButton)

        loginBtn.setOnClickListener {
            val email = emailET.text.toString().trim()
            val password = passwordET.text.toString().trim()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                showLoading(true)
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                    showLoading(false)
                    if (it.isSuccessful) navigateToHome()
                    else Toast.makeText(this, "Error: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        registerBtn.setOnClickListener {
            val email = emailET.text.toString().trim()
            val password = passwordET.text.toString().trim()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                showLoading(true)
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                    showLoading(false)
                    if (it.isSuccessful) navigateToHome()
                    else Toast.makeText(this, "Error: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        googleBtn.setOnClickListener {
            showLoading(true)
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            showLoading(false)
            if (task.isSuccessful) {
                navigateToHome()
            } else {
                Log.e("FirebaseAuth", "Error: ${task.exception?.message}")
                Toast.makeText(this, "Error al autenticar con Firebase", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) navigateToHome()
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
