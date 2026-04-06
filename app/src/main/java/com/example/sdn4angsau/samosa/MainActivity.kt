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

    companion object {
        private const val VALID_USERNAME = "admin123"
        private const val VALID_PASSWORD = "admin123"
    }

    private lateinit var binding: ActivityMainBinding

    // Variabel untuk Firebase dan Google Sign-In
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inisialisasi Firebase Auth
        auth = FirebaseAuth.getInstance()

        // 2. CEK INGATAN (SESSION) SEBELUM MEMUAT TAMPILAN
        val sharedPref = getSharedPreferences("SesiSamosa", Context.MODE_PRIVATE)
        val sudahLoginLokal = sharedPref.getBoolean("SUDAH_LOGIN", false)
        val currentUser = auth.currentUser

        // Jika sudah ada sesi lokal ATAU sudah login di Firebase, langsung ke Dashboard
        if (sudahLoginLokal || currentUser != null) {
            pindahKeDashboard()
            return
        }

        // 3. JIKA BELUM LOGIN, TAMPILKAN HALAMAN MASUK
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 4. Konfigurasi Mesin Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // default_web_client_id ini otomatis dibuat oleh google-services.json dari Firebase
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // ==========================================
        // LOGIKA TOMBOL MASUK BIASA (TOMBOL HIJAU)
        // ==========================================
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            val errorMessage = when {
                username.isEmpty() || password.isEmpty() -> getString(R.string.login_error_empty_fields)
                username != VALID_USERNAME && password != VALID_PASSWORD -> getString(R.string.login_error_wrong_credentials)
                username != VALID_USERNAME -> getString(R.string.login_error_wrong_username)
                password != VALID_PASSWORD -> getString(R.string.login_error_wrong_password)
                else -> null
            }

            if (errorMessage != null) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            } else {
                sharedPref.edit().putBoolean("SUDAH_LOGIN", true).apply()
                pindahKeDashboard()
            }
        }

        // ==========================================
        // LOGIKA TOMBOL MASUK DENGAN GOOGLE (TOMBOL PUTIH)
        // ==========================================
        binding.btnGoogleLogin.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            launcherGoogleSignIn.launch(signInIntent)
        }
    }

    // Mesin penangkap hasil setelah user memilih akun Google di HP
    private val launcherGoogleSignIn = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // KITA HAPUS SYARAT 'RESULT_OK' AGAR SEMUA ERROR BISA TERTANGKAP DAN MUNCUL DI LAYAR
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            // Berhasil memilih akun Google, sekarang hubungkan dengan Firebase
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                hubungkanKeFirebaseDenganGoogle(idToken)
            }
        } catch (e: ApiException) {
            // INI AKAN MEMUNCULKAN KODE ANGKA PENYEBAB ASLINYA
            Toast.makeText(this, "Google Sign-In Gagal (Kode: ${e.statusCode})", Toast.LENGTH_LONG).show()
        }
    }

    // Fungsi untuk mendaftarkan akun Google yang dipilih ke Firebase Authentication
    private fun hubungkanKeFirebaseDenganGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login Firebase Sukses! Simpan ingatan ke sistem
                    val sharedPref = getSharedPreferences("SesiSamosa", Context.MODE_PRIVATE)
                    sharedPref.edit().putBoolean("SUDAH_LOGIN", true).apply()

                    Toast.makeText(this, "Berhasil masuk dengan Google!", Toast.LENGTH_SHORT).show()
                    pindahKeDashboard()
                } else {
                    Toast.makeText(this, "Gagal terhubung ke sistem: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Fungsi jalan pintas untuk pindah layar
    private fun pindahKeDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish() // Hancurkan halaman login agar tidak bisa di-back
    }
}
