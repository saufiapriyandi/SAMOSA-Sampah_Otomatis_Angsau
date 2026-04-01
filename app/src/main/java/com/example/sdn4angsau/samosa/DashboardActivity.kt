package com.example.sdn4angsau.samosa

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sdn4angsau.samosa.databinding.ActivityDashboardBinding

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dataContoh = listOf(
            TempatSampah("3", "Laboratorium", 100),
            TempatSampah("4", "Ruang Kantor", 95),
            TempatSampah("5", "Kantin SDN 4", 78),
            TempatSampah("1", "Perpustakaan", 45),
            TempatSampah("2", "Ruang Guru", 12)
        )

        val sampahAdapter = TempatSampahAdapter(dataContoh)

        binding.rvTempatSampah.layoutManager = LinearLayoutManager(this)
        binding.rvTempatSampah.adapter = sampahAdapter

        // Perintah untuk tombol keluar
        binding.btnLogout.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}