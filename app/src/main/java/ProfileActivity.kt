package com.example.sdn4angsau.samosa

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.example.sdn4angsau.samosa.databinding.ActivityProfileBinding

// Tambahan Import wajib untuk fungsi Logout Google dan Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // MEMAKSA IKON JAM & BATERAI MENJADI PUTIH TERANG
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        binding.btnBackProfile.setOnClickListener {
            finish()
        }

        binding.btnTutorialProfile.setOnClickListener {
            startActivity(Intent(this, TutorialActivity::class.java))
        }

        // FUNGSI TOMBOL LOGOUT
        binding.btnLogoutProfile.setOnClickListener {
            // 1. Hapus sesi di Firebase
            FirebaseAuth.getInstance().signOut()

            // 2. Hapus sesi Google (agar saat login lagi disuruh milih email)
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            GoogleSignIn.getClient(this, gso).signOut()

            // 3. Hapus ingatan / sesi lokal yang kita buat manual
            val sharedPref = getSharedPreferences("SesiSamosa", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putBoolean("SUDAH_LOGIN", false)
            editor.apply()

            // 4. Pindah ke halaman Login (MainActivity) dan HANCURKAN SEMUA RIWAYAT
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
