package com.example.sdn4angsau.samosa

import android.os.Bundle
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

        // Fungsi untuk tombol kembali yang baru (btnBackProfile)
        binding.btnBackProfile.setOnClickListener {
            finish() // Perintah ini akan menutup halaman profil dan kembali ke halaman sebelumnya
        }
    }
}