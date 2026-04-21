package com.example.sdn4angsau.samosa

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extraTopPadding = (12 * resources.displayMetrics.density).toInt()
            val extraBottomPadding = (24 * resources.displayMetrics.density).toInt()

            binding.headerDetailBar.updatePadding(top = systemBars.top + extraTopPadding)
            binding.scrollDetail.updatePadding(bottom = systemBars.bottom + extraBottomPadding)

            insets
        }

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        val tanggalSekarang = dateFormat.format(calendar.time)
        binding.tvTanggal.text = tanggalSekarang

        val currentDayIndex = TempatSampahHistoryHelper.getCurrentSchoolDayIndex(calendar)

        val namaLokasi = intent.getStringExtra("EXTRA_LOKASI") ?: getString(R.string.detail_unknown_location)
        val binId = intent.getStringExtra("EXTRA_BINID") ?: "-"
        val persentaseAsliKini = intent.getIntExtra("EXTRA_PERSENTASE", 0)
        val bin = TempatSampah(
            binId = binId,
            lokasi = namaLokasi,
            persentase = persentaseAsliKini,
            isActive = true
        )

        binding.tvDetailLokasi.text = namaLokasi
        binding.tvDetailBinId.text = getString(R.string.detail_bin_id_format, binId)

        val kumpulanData = List(5) { dayIndex ->
            TempatSampahHistoryHelper.getDailyPercentages(
                bin = bin,
                dayIndex = dayIndex,
                includeCurrentReading = true
            )
        }
        val buttons = listOf(binding.btnSenin, binding.btnSelasa, binding.btnRabu, binding.btnKamis, binding.btnJumat)
        val texts = listOf(binding.tvSenin, binding.tvSelasa, binding.tvRabu, binding.tvKamis, binding.tvJumat)

        buttons[currentDayIndex].setCardBackgroundColor(colorSelected)
        texts[currentDayIndex].setTextColor(colorWhite)
        updateSemuaGrafik(kumpulanData[currentDayIndex], isHariIni = true)

        for (i in buttons.indices) {
            buttons[i].setOnClickListener {
                for (j in buttons.indices) {
                    buttons[j].setCardBackgroundColor(colorBackground)
                    texts[j].setTextColor(colorMutedText)
                }

                buttons[i].setCardBackgroundColor(colorSelected)
                texts[i].setTextColor(colorWhite)

                val apakahHariIni = i == currentDayIndex
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

        val colorInt = when {
            persentase >= 90 -> colorFull
            persentase >= 60 -> colorWarning
            else -> colorSafe
        }
        tvVal.setTextColor(colorInt)
        barCard.setCardBackgroundColor(colorInt)

        val tinggiGrafik = if (persentase < 10) 10 else persentase
        val layoutParams = barCard.layoutParams
        val density = resources.displayMetrics.density
        layoutParams.height = (tinggiGrafik * density).toInt()
        barCard.layoutParams = layoutParams
    }

    companion object {
        private val colorBackground = Color.parseColor("#F5F7F9")
        private val colorMutedText = Color.parseColor("#888888")
        private val colorSelected = Color.parseColor("#20B273")
        private val colorWhite = Color.parseColor("#FFFFFF")
        private val colorSafe = Color.parseColor("#20B273")
        private val colorWarning = Color.parseColor("#FFA500")
        private val colorFull = Color.parseColor("#FF4B4B")
    }
}
