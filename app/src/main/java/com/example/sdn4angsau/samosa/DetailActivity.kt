package com.example.sdn4angsau.samosa

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.sdn4angsau.samosa.databinding.ActivityDetailBinding

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Menerima data yang dikirim dari Dasbor
        val namaLokasi = intent.getStringExtra("EXTRA_LOKASI") ?: "Lokasi Tidak Diketahui"
        val binId = intent.getStringExtra("EXTRA_BINID") ?: "-"
        val persentase = intent.getIntExtra("EXTRA_PERSENTASE", 0)

        // 2. Memasang Teks Judul
        binding.tvDetailLokasi.text = namaLokasi
        binding.tvDetailBinId.text = "BIN-ID: $binId"

        // 3. Memasang Data ke Grafik "Kini" (Paling Kanan)
        binding.tvGrafikSekarang.text = "$persentase%"

        // Menyesuaikan tinggi kotak grafik berdasarkan persentase (Maksimal 110dp)
        val tinggiGrafik = if (persentase < 10) 10 else persentase // Minimal 10dp agar tidak hilang
        val layoutParams = binding.barGrafikSekarang.layoutParams
        // Mengubah dp ke pixels
        val density = resources.displayMetrics.density
        layoutParams.height = (tinggiGrafik * density).toInt()
        binding.barGrafikSekarang.layoutParams = layoutParams

        // 4. Mewarnai Grafik dan Log berdasarkan Persentase
        when {
            persentase >= 90 -> {
                binding.tvGrafikSekarang.setTextColor(Color.parseColor("#FF4B4B"))
                binding.barGrafikSekarang.setCardBackgroundColor(Color.parseColor("#FF4B4B"))
                binding.tvLogTerbaruStatus.text = "Status: PENUH ($persentase%)"
            }
            persentase >= 60 -> {
                binding.tvGrafikSekarang.setTextColor(Color.parseColor("#FFA500"))
                binding.barGrafikSekarang.setCardBackgroundColor(Color.parseColor("#FFA500"))
                binding.tvLogTerbaruStatus.text = "Status: WASPADA ($persentase%)"
            }
            else -> {
                binding.tvGrafikSekarang.setTextColor(Color.parseColor("#20B273"))
                binding.barGrafikSekarang.setCardBackgroundColor(Color.parseColor("#20B273"))
                binding.tvLogTerbaruStatus.text = "Status: AMAN ($persentase%)"
            }
        }

        // 5. Tombol Kembali
        binding.btnBackDetail.setOnClickListener {
            finish()
        }
    }
}