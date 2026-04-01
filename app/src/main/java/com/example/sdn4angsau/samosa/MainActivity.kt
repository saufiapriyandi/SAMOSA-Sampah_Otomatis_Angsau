package com.example.sdn4angsau.samosa

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.sdn4angsau.samosa.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Memuat desain UI ke layar menggunakan ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Memberikan perintah pada tombol saat diklik
        binding.btnLogin.setOnClickListener {
            // Mengambil teks dari kolom input dan membuang spasi tak sengaja di ujungnya
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Mengecek apakah isiannya sesuai dengan rancangan sistemmu
            if (username == "admin123" && password == "admin123") {
                // Jika benar, pindah ke halaman Dashboard
                val intent = Intent(this, DashboardActivity::class.java)
                startActivity(intent)

                // Menutup halaman login agar pengguna tidak bisa kembali menggunakan tombol back HP
                finish()
            } else {
                // Jika salah atau kosong, munculkan peringatan kecil
                Toast.makeText(this, "Username atau Password salah!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}