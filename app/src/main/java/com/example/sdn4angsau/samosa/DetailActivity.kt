package com.example.sdn4angsau.samosa

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.example.sdn4angsau.samosa.databinding.ActivityDetailBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var database: DatabaseReference
    private var kumpulanData: MutableList<MutableList<Int>> = mutableListOf()
    private var currentDayIndex = 0
    private var lastStatusTutup = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi Firebase Database Root
        database = FirebaseDatabase.getInstance().reference

        // Setup Window Insets (Padding Status Bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extraTopPadding = (12 * resources.displayMetrics.density).toInt()
            val extraBottomPadding = (24 * resources.displayMetrics.density).toInt()

            binding.headerDetailBar.updatePadding(top = systemBars.top + extraTopPadding)
            binding.scrollDetail.updatePadding(bottom = systemBars.bottom + extraBottomPadding)
            insets
        }
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        // Setup Tanggal Hari Ini
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        binding.tvTanggal.text = dateFormat.format(calendar.time)

        // Ambil Data Lokasi dari Intent
        currentDayIndex = TempatSampahHistoryHelper.getCurrentSchoolDayIndex(calendar)
        val namaLokasi = intent.getStringExtra("EXTRA_LOKASI") ?: "Tempat Sampah SDN 4 Angsau"
        val binId = intent.getStringExtra("EXTRA_BINID") ?: "Samosa_01"

        binding.tvDetailLokasi.text = namaLokasi
        binding.tvDetailBinId.text = "BIN-ID: $binId"

        // Inisialisasi Data Grafik Mockup
        val bin = TempatSampah(binId = binId, lokasi = namaLokasi, persentase = 0, isActive = true)
        kumpulanData = MutableList(5) { dayIndex ->
            TempatSampahHistoryHelper.getDailyPercentages(bin, dayIndex, true).toMutableList()
        }

        setupTabTombol()
        mulaiPantauFirebase()

        binding.btnBackDetail.setOnClickListener { finish() }

        // Pindah ke Halaman Riwayat Penuh
        binding.btnLihatRiwayat.setOnClickListener {
            startActivity(Intent(this, RiwayatActivity::class.java))
        }
    }

    private fun setupTabTombol() {
        val buttons = listOf(binding.btnSenin, binding.btnSelasa, binding.btnRabu, binding.btnKamis, binding.btnJumat)
        val texts = listOf(binding.tvSenin, binding.tvSelasa, binding.tvRabu, binding.tvKamis, binding.tvJumat)

        // Reset semua tombol ke warna abu-abu (Mencegah Bug Kamis-Jumat Hijau bareng)
        for (i in buttons.indices) {
            buttons[i].setCardBackgroundColor(colorBackground)
            texts[i].setTextColor(colorMutedText)
        }

        // Set warna Hijau untuk hari aktif saat ini
        buttons[currentDayIndex].setCardBackgroundColor(colorSelected)
        texts[currentDayIndex].setTextColor(colorWhite)
        updateSemuaGrafik(kumpulanData[currentDayIndex], isHariIni = true)

        // Logika Klik Tab Hari
        for (i in buttons.indices) {
            buttons[i].setOnClickListener {
                for (j in buttons.indices) {
                    buttons[j].setCardBackgroundColor(colorBackground)
                    texts[j].setTextColor(colorMutedText)
                }
                buttons[i].setCardBackgroundColor(colorSelected)
                texts[i].setTextColor(colorWhite)

                val apakahHariIni = (i == currentDayIndex)
                updateSemuaGrafik(kumpulanData[i], apakahHariIni)
            }
        }
    }

    private fun mulaiPantauFirebase() {
        // MENGARAHKAN KE FOLDER "Tempat_Sampah_1" SESUAI STRUKTUR FIREBASE ALAT
        val dbSampah = FirebaseDatabase.getInstance().getReference("Tempat_Sampah_1")

        // 1. Pantau Kapasitas (Kunci: kapasitas_persen)
        dbSampah.child("kapasitas_persen").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue(Int::class.java) ?: snapshot.getValue(Long::class.java)?.toInt() ?: 0
                kumpulanData[currentDayIndex][4] = value
                updateSemuaGrafik(kumpulanData[currentDayIndex], true)
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 2. Pantau Jarak Sensor Dalam (Kunci: jarak_cm)
        dbSampah.child("jarak_cm").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val jarak = snapshot.getValue(Int::class.java) ?: snapshot.getValue(Long::class.java)?.toInt() ?: 0
                binding.tvSensor2.text = "$jarak cm"
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 3. Pantau Status Penutup (Kunci: status_tutup)
        dbSampah.child("status_tutup").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val statusRaw = snapshot.getValue(String::class.java) ?: "TERTUTUP"
                val status = statusRaw.uppercase()

                binding.tvStatusPenutup.text = status
                if (status == "TERBUKA") {
                    binding.tvStatusPenutup.setTextColor(colorWarning)
                } else {
                    binding.tvStatusPenutup.setTextColor(colorSafe)
                }

                // Logika Pencatat Riwayat Otomatis saat status berubah
                if (lastStatusTutup.isNotEmpty() && lastStatusTutup != status) {
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
                    val waktuSekarang = dateFormat.format(Calendar.getInstance().time)

                    val pesan = if (status == "TERBUKA") "Tempat sampah digunakan (Tutup Terbuka)." else "Tempat sampah selesai digunakan."
                    val tipe = if (status == "TERBUKA") "warning" else "success"

                    // Simpan Log ke Root "Logs" agar RiwayatActivity bisa baca
                    val logRef = FirebaseDatabase.getInstance().getReference("Logs").push()
                    logRef.setValue(mapOf("pesan" to pesan, "waktu" to waktuSekarang, "tipe" to tipe))
                }
                lastStatusTutup = status
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateSemuaGrafik(dataHarian: List<Int>, isHariIni: Boolean) {
        aturBalok(binding.val08, binding.bar08, dataHarian[0])
        aturBalok(binding.val10, binding.bar10, dataHarian[1])
        aturBalok(binding.val12, binding.bar12, dataHarian[2])
        aturBalok(binding.val14, binding.bar14, dataHarian[3])
        aturBalok(binding.tvGrafikSekarang, binding.barGrafikSekarang, dataHarian[4])

        binding.tvWaktuKini.text = if (isHariIni) "Kini" else "Akhir"

        val persentaseKini = dataHarian[4]
        val (statusLabel, warnaStatus) = when {
            persentaseKini >= 90 -> Pair("PENUH", colorFull)
            persentaseKini >= 60 -> Pair("PERINGATAN", colorWarning)
            else -> Pair("AMAN", colorSafe)
        }
        binding.tvLogTerbaruStatus.text = "Status: $statusLabel ($persentaseKini%)"
        binding.tvLogTerbaruStatus.setTextColor(warnaStatus)
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

        // Skala Tinggi Bar
        val tinggiVisual = 12 + (persentase * 1.1).toInt()
        val layoutParams = barCard.layoutParams
        layoutParams.height = (tinggiVisual * resources.displayMetrics.density).toInt()
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