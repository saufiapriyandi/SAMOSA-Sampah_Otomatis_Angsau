package com.example.sdn4angsau.samosa

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extraTopPadding = (12 * resources.displayMetrics.density).toInt()
            val extraBottomPadding = (24 * resources.displayMetrics.density).toInt()

            binding.headerProfileBar.updatePadding(top = systemBars.top + extraTopPadding)
            binding.scrollProfile.updatePadding(bottom = systemBars.bottom + extraBottomPadding)

            insets
        }

        // MEMAKSA IKON JAM & BATERAI MENJADI PUTIH TERANG
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        binding.btnBackProfile.setOnClickListener {
            finish()
        }

        binding.btnTutorialProfile.setOnClickListener {
            startActivity(Intent(this, TutorialActivity::class.java))
        }

        binding.btnManageBinsProfile.setOnClickListener {
            startActivity(Intent(this, BinManagementActivity::class.java))
        }

        binding.btnReportProfile.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
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
            val sharedPref = getSharedPreferences("SesiSamosa", MODE_PRIVATE)
            sharedPref.edit {
                putBoolean("SUDAH_LOGIN", false)
            }

            // 4. Pindah ke halaman Login (MainActivity) dan HANCURKAN SEMUA RIWAYAT
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
