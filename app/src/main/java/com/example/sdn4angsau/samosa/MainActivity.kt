package com.example.sdn4angsau.samosa

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.sdn4angsau.samosa.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Confidentiality: Menggunakan EncryptedSharedPreferences untuk menyimpan sesi
        val securePrefs = SecurityHelper.getEncryptedPrefs(this)
        
        auth = FirebaseAuth.getInstance()
        val sudahLoginLokal = securePrefs.getBoolean("SUDAH_LOGIN", false)
        val currentUser = auth.currentUser

        // Cek sesi sebelum merender UI
        if (sudahLoginLokal || currentUser != null) {
            pindahKeDashboard()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleSignIn()
        setupLoginListeners(securePrefs)
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupLoginListeners(securePrefs: android.content.SharedPreferences) {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan Password tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Autentikasi dengan Firebase Auth menggunakan Email dan Password
            auth.signInWithEmailAndPassword(username, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        securePrefs.edit().putBoolean("SUDAH_LOGIN", true).apply()
                        pindahKeDashboard()
                    } else {
                        Toast.makeText(this, "Gagal login: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        binding.btnGoogleLogin.setOnClickListener {
            launcherGoogleSignIn.launch(googleSignInClient.signInIntent)
        }
    }

    private val launcherGoogleSignIn = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let(::hubungkanKeFirebaseDenganGoogle)
        } catch (e: ApiException) {
            Toast.makeText(this, "Google Sign-In Gagal", Toast.LENGTH_LONG).show()
        }
    }

    private fun hubungkanKeFirebaseDenganGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    SecurityHelper.getEncryptedPrefs(this).edit().putBoolean("SUDAH_LOGIN", true).apply()
                    pindahKeDashboard()
                } else {
                    Toast.makeText(this, "Firebase Gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun pindahKeDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}
