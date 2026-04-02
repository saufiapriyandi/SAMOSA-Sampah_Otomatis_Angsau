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

        // 1. Mengukur ukuran Poni HP dan menerapkannya ke Jarak Atas serta Kotak Buatan
        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top

            // Memberi jarak pada konten utama
            val extraPadding = (16 * resources.displayMetrics.density).toInt()
            view.setPadding(view.paddingLeft, statusBarHeight + extraPadding, view.paddingRight, view.paddingBottom)

            // Mengatur tinggi kotak hijau buatan kita agar sama persis dengan tinggi Poni HP
            val layoutParams = binding.fakeStatusBar.layoutParams
            layoutParams.height = statusBarHeight
            binding.fakeStatusBar.layoutParams = layoutParams

            insets
        }

        // Memastikan ikon status bar selalu putih agar kontras dengan hijau
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        // 2. EFEK MEMUDAR (FADE) PADA KOTAK BUATAN SAAT DIGULIR
        binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            // Menghitung transparansi berdasarkan posisi scroll (0 sampai 150)
            val alpha = (scrollY / 150f).coerceIn(0f, 1f)

            // Terapkan transparansi ke kotak buatan
            binding.fakeStatusBar.alpha = alpha

            // Tambahan: beri bayangan (elevation) sedikit saat sudah pekat
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

        binding.btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.btnLogout.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

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