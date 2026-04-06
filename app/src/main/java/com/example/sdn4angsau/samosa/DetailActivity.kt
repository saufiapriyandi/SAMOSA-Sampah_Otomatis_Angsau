package com.example.sdn4angsau.samosa

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import android.widget.TextView
import androidx.core.view.WindowInsetsControllerCompat
import com.example.sdn4angsau.samosa.databinding.ActivityDetailBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        val tanggalSekarang = dateFormat.format(calendar.time)
        binding.tvTanggal.text = tanggalSekarang

        val hariIniAsli = calendar.get(Calendar.DAY_OF_WEEK)

        val namaLokasi = intent.getStringExtra("EXTRA_LOKASI") ?: getString(R.string.detail_unknown_location)
        val binId = intent.getStringExtra("EXTRA_BINID") ?: "-"
        val persentaseAsliKini = intent.getIntExtra("EXTRA_PERSENTASE", 0)

        binding.tvDetailLokasi.text = namaLokasi
        binding.tvDetailBinId.text = getString(R.string.detail_bin_id_format, binId)

        val dataSenin = listOf(0, 15, 30, 45, 50)
        val dataSelasa = listOf(10, 40, 70, 95, 100)
        val dataRabu = listOf(5, 20, 35, 55, 60)
        val dataKamis = listOf(10, 35, 65, 85, persentaseAsliKini)
        val dataJumat = listOf(0, 5, 15, 25, 40)

        val kumpulanData = listOf(dataSenin, dataSelasa, dataRabu, dataKamis, dataJumat)
        val buttons = listOf(binding.btnSenin, binding.btnSelasa, binding.btnRabu, binding.btnKamis, binding.btnJumat)
        val texts = listOf(binding.tvSenin, binding.tvSelasa, binding.tvRabu, binding.tvKamis, binding.tvJumat)

        updateSemuaGrafik(dataKamis, isHariIni = (hariIniAsli == Calendar.THURSDAY))

        for (i in buttons.indices) {
            buttons[i].setOnClickListener {
                for (j in buttons.indices) {
                    buttons[j].setCardBackgroundColor(Color.parseColor("#F5F7F9"))
                    texts[j].setTextColor(Color.parseColor("#888888"))
                }

                buttons[i].setCardBackgroundColor(Color.parseColor("#20B273"))
                texts[i].setTextColor(Color.parseColor("#FFFFFF"))

                val apakahHariIni = (i + 2) == hariIniAsli
                updateSemuaGrafik(kumpulanData[i], apakahHariIni)
            }
        }

        binding.btnBackDetail.setOnClickListener {
            finish()
        }
    }

    private fun updateSemuaGrafik(dataHarian: List<Int>, isHariIni: Boolean) {
        aturBalok(binding.val08, binding.bar08, dataHarian[0])
        aturBalok(binding.val10, binding.bar10, dataHarian[1])
        aturBalok(binding.val12, binding.bar12, dataHarian[2])
        aturBalok(binding.val14, binding.bar14, dataHarian[3])
        aturBalok(binding.tvGrafikSekarang, binding.barGrafikSekarang, dataHarian[4])

        if (isHariIni) {
            binding.tvWaktuKini.text = getString(R.string.detail_now_label)
        } else {
            binding.tvWaktuKini.text = getString(R.string.detail_last_reading_label)
        }

        val persentaseKini = dataHarian[4]
        val statusLabel = when {
            persentaseKini >= 90 -> getString(R.string.detail_status_full)
            persentaseKini >= 60 -> getString(R.string.detail_status_warning)
            else -> getString(R.string.detail_status_safe)
        }
        binding.tvLogTerbaruStatus.text =
            getString(R.string.detail_status_format, statusLabel, persentaseKini)
    }

    private fun aturBalok(tvVal: TextView, barCard: CardView, persentase: Int) {
        tvVal.text = "$persentase%"

        val colorHex = when {
            persentase >= 90 -> "#FF4B4B"
            persentase >= 60 -> "#FFA500"
            else -> "#20B273"
        }
        tvVal.setTextColor(Color.parseColor(colorHex))
        barCard.setCardBackgroundColor(Color.parseColor(colorHex))

        val tinggiGrafik = if (persentase < 10) 10 else persentase
        val layoutParams = barCard.layoutParams
        val density = resources.displayMetrics.density
        layoutParams.height = (tinggiGrafik * density).toInt()
        barCard.layoutParams = layoutParams
    }
}
