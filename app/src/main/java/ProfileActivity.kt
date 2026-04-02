package com.example.sdn4angsau.samosa

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.sdn4angsau.samosa.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tombol Kembali ke Dashboard
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Tombol Tutorial (Bisa dihubungkan ke activity tutorial nanti)
        binding.btnTutorial.setOnClickListener {
            Toast.makeText(this, "Fitur Tutorial akan segera hadir", Toast.LENGTH_SHORT).show()
        }

        // Tombol Logout
        binding.btnLogoutFromProfile.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            // Menghapus semua riwayat halaman agar tidak bisa di-back
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}