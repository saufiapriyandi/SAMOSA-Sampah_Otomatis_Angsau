package com.example.sdn4angsau.samosa

import android.content.Context
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

        // Inisialisasi Firebase & cek sesi lokal sebelum merender form login.
        auth = FirebaseAuth.getInstance()
        val sharedPref = getSharedPreferences("SesiSamosa", Context.MODE_PRIVATE)
        val sudahLoginLokal = sharedPref.getBoolean("SUDAH_LOGIN", false)
        val currentUser = auth.currentUser

        if (sudahLoginLokal || currentUser != null) {
            pindahKeDashboard()
            return
        }

        // Tampilkan UI login.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.btnLogin.setOnClickListener {
            val blockExpiryTime = sharedPref.getLong("BLOCK_EXPIRY_TIME", 0L)
            val currentTime = System.currentTimeMillis()

            if (blockExpiryTime > currentTime) {
                val remainingMinutes = ((blockExpiryTime - currentTime) / 60000) + 1
                Toast.makeText(this, "Terlalu banyak percobaan gagal. Silakan coba lagi dalam $remainingMinutes menit.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            } else if (blockExpiryTime != 0L) {
                // Blokir sudah kedaluwarsa, reset counter
                sharedPref.edit().putInt("FAILED_LOGIN_ATTEMPTS", 0).putLong("BLOCK_EXPIRY_TIME", 0L).apply()
            }

            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username == "admin123" && password == "admin123") {
                sharedPref.edit().putBoolean("SUDAH_LOGIN", true).putInt("FAILED_LOGIN_ATTEMPTS", 0).apply()
                pindahKeDashboard()
            } else {
                var failedAttempts = sharedPref.getInt("FAILED_LOGIN_ATTEMPTS", 0)
                failedAttempts++
                
                if (failedAttempts >= 3) {
                    val newBlockExpiryTime = System.currentTimeMillis() + (15 * 60 * 1000) // 15 menit
                    sharedPref.edit().putInt("FAILED_LOGIN_ATTEMPTS", failedAttempts).putLong("BLOCK_EXPIRY_TIME", newBlockExpiryTime).apply()
                    Toast.makeText(this, "Akses diblokir karena 3 kali gagal. Coba lagi dalam 15 menit.", Toast.LENGTH_LONG).show()
                } else {
                    sharedPref.edit().putInt("FAILED_LOGIN_ATTEMPTS", failedAttempts).apply()
                    val sisaPercobaan = 3 - failedAttempts
                    Toast.makeText(this, "Username atau Password salah. Sisa percobaan: $sisaPercobaan", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnGoogleLogin.setOnClickListener {
            launcherGoogleSignIn.launch(googleSignInClient.signInIntent)
        }
    }

    private fun pindahKeDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private val launcherGoogleSignIn = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let(::hubungkanKeFirebaseDenganGoogle)
        } catch (e: ApiException) {
            Toast.makeText(this, "Google Sign-In Gagal (Kode: ${e.statusCode})", Toast.LENGTH_LONG).show()
        }
    }

    private fun hubungkanKeFirebaseDenganGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val sharedPref = getSharedPreferences("SesiSamosa", Context.MODE_PRIVATE)
                    sharedPref.edit().putBoolean("SUDAH_LOGIN", true).apply()
                    Toast.makeText(this, "Berhasil masuk dengan Google!", Toast.LENGTH_SHORT).show()
                    pindahKeDashboard()
                } else {
                    Toast.makeText(this, "Gagal terhubung ke sistem: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
