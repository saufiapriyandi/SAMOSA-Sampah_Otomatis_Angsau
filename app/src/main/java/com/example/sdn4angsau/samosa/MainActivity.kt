package com.example.sdn4angsau.samosa

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sdn4angsau.samosa.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. CEK INGATAN (SESSION) SEBELUM MEMUAT TAMPILAN
        val sharedPref = getSharedPreferences("SesiSamosa", Context.MODE_PRIVATE)
        val sudahLogin = sharedPref.getBoolean("SUDAH_LOGIN", false)

        if (sudahLogin) {
            // Jika sudah login, lompat ke Dasbor dan hentikan proses di sini
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // 2. JIKA BELUM LOGIN, TAMPILKAN HALAMAN MASUK
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            // Simpan ingatan bahwa user sudah berhasil login
            val editor = sharedPref.edit()
            editor.putBoolean("SUDAH_LOGIN", true)
            editor.apply()

            // Pergi ke dasbor
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}