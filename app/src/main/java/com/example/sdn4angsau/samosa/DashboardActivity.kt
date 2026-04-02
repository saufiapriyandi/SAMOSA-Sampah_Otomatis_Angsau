package com.example.sdn4angsau.samosa

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sdn4angsau.samosa.databinding.ActivityDashboardBinding

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. MENDETEKSI PONI HP (ATAS) & TOMBOL NAVIGASI (BAWAH)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Jarak Atas
            val extraPaddingTop = (16 * resources.displayMetrics.density).toInt()
            binding.topBar.setPadding(binding.topBar.paddingLeft, systemBars.top + extraPaddingTop, binding.topBar.paddingRight, binding.topBar.paddingBottom)

            // Kotak Status Bar Buatan
            val layoutParams = binding.fakeStatusBar.layoutParams
            layoutParams.height = systemBars.top
            binding.fakeStatusBar.layoutParams = layoutParams

            // Jarak Bawah
            binding.scrollView.setPadding(binding.scrollView.paddingLeft, binding.scrollView.paddingTop, binding.scrollView.paddingRight, systemBars.bottom)

            insets
        }

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        // 2. EFEK MEMUDAR (FADE) STATUS BAR
        binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            val alpha = (scrollY / 150f).coerceIn(0f, 1f)
            binding.fakeStatusBar.alpha = alpha

            if (alpha == 1f) {
                binding.fakeStatusBar.elevation = 8f
            } else {
                binding.fakeStatusBar.elevation = 0f
            }
        })

        // --- DAFTAR DATA SAMPAH ---
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

        // PERINTAH KLIK PROFIL
        binding.btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // PENCARIAN
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val keyword = s.toString().lowercase()
                val hasilFilter = dataContoh.filter {
                    it.lokasi.lowercase().contains(keyword)
                }
                sampahAdapter.updateData(hasilFilter)
            }
        })
    }
}