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

    // Variabel untuk melacak status terakhir agar tidak duplikat
    private var lastStatusTutup = ""
    private var lastKapasitasStatus = ""

    // Variabel penampung nilai sensor terakhir untuk ditulis ke dalam log riwayat
    private var latestKapasitas = 0
    private var latestJarakDalam = 0
    private var latestJarakLuar = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference

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
        binding.tvTanggal.text = dateFormat.format(calendar.time)

        // Konversi penentuan indeks hari Android agar mendukung 7 hari (0=Senin ... 6=Minggu)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        currentDayIndex = when (dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        val namaLokasi = intent.getStringExtra("EXTRA_LOKASI") ?: "Tempat Sampah SDN 4 Angsau"
        val binId = intent.getStringExtra("EXTRA_BINID") ?: "Samosa_01"

        binding.tvDetailLokasi.text = namaLokasi
        binding.tvDetailBinId.text = "BIN-ID: $binId"

        // Inisialisasi dengan nol — data nyata akan diisi dari Firebase
        kumpulanData = MutableList(7) { MutableList(5) { 0 } }

        setupTabTombol()
        mulaiPantauFirebase(binId)
        bacaRekapitulasiHarian(binId)

        binding.btnBackDetail.setOnClickListener { finish() }

        binding.btnLihatRiwayat.setOnClickListener {
            startActivity(Intent(this, RiwayatActivity::class.java))
        }
    }

    private fun setupTabTombol() {
        val buttons = listOf(
            binding.btnSenin, binding.btnSelasa, binding.btnRabu,
            binding.btnKamis, binding.btnJumat, binding.btnSabtu, binding.btnMinggu
        )
        val texts = listOf(
            binding.tvSenin, binding.tvSelasa, binding.tvRabu,
            binding.tvKamis, binding.tvJumat, binding.tvSabtu, binding.tvMinggu
        )

        // Setel semua tab ke default redup
        for (i in buttons.indices) {
            buttons[i].setCardBackgroundColor(colorBackground)
            texts[i].setTextColor(colorMutedText)
        }

        // Aktifkan tab hari ini
        buttons[currentDayIndex].setCardBackgroundColor(colorSelected)
        texts[currentDayIndex].setTextColor(colorWhite)
        updateSemuaGrafik(kumpulanData[currentDayIndex], isHariIni = true)

        // Listener saat di klik
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

    private fun mulaiPantauFirebase(binId: String) {
        val (dbSampah, fieldName) = if (binId == "Tempat_Sampah_1") {
            Pair(FirebaseDatabase.getInstance().getReference("Tempat_Sampah_1"), "kapasitas_persen")
        } else {
            Pair(FirebaseDatabase.getInstance().getReference("tempat_sampah/$binId"), "persentase")
        }

        // 1. Pantau Kapasitas
        dbSampah.child(fieldName).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // [FIX M-1] Validasi nilai numerik: batasi ketat 0-100
                val value = (snapshot.getValue(Int::class.java)
                    ?: snapshot.getValue(Long::class.java)?.toInt()
                    ?: 0).coerceIn(0, 100)
                latestKapasitas = value

                // Update slot "Kini" (indeks 4) dengan nilai real-time
                kumpulanData[currentDayIndex][4] = value
                updateSemuaGrafik(kumpulanData[currentDayIndex], true)

                // Simpan nilai ini ke slot waktu yang sesuai di Firebase
                simpanRekapitulasiHarian(binId, value)

                val currentKapasitasStatus = when {
                    value >= 90 -> "PENUH"
                    value >= 60 -> "PERINGATAN"
                    else -> "AMAN"
                }

                if (lastKapasitasStatus.isNotEmpty() && lastKapasitasStatus != currentKapasitasStatus) {
                    if (currentKapasitasStatus == "PENUH" || currentKapasitasStatus == "PERINGATAN") {
                        simpanRiwayatKeFirebase(
                            pesan = "Kapasitas mencapai $value% ($currentKapasitasStatus).\nJarak tumpukan: $latestJarakDalam cm.",
                            tipe = if (currentKapasitasStatus == "PENUH") "danger" else "warning"
                        )
                    } else if (currentKapasitasStatus == "AMAN" && lastKapasitasStatus == "PENUH") {
                        simpanRiwayatKeFirebase(
                            pesan = "Tempat sampah telah dikosongkan.\nKapasitas saat ini: $value%.",
                            tipe = "success"
                        )
                    }
                }
                lastKapasitasStatus = currentKapasitasStatus
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 2. Pantau Jarak Sensor Dalam Bak
        dbSampah.child("jarak_cm").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // [FIX M-1] Batasi nilai jarak: 0-999 cm (tidak mungkin negatif atau sangat besar)
                val jarak = (snapshot.getValue(Int::class.java)
                    ?: snapshot.getValue(Long::class.java)?.toInt()
                    ?: 0).coerceIn(0, 999)
                latestJarakDalam = jarak
                binding.tvSensor2.text = "$jarak cm"
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 3. Pantau Jarak Sensor Luar Bak
        dbSampah.child("jarak_luar_cm").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // [FIX M-1] Batasi nilai jarak: 0-999 cm
                val jarakLuar = (snapshot.getValue(Int::class.java)
                    ?: snapshot.getValue(Long::class.java)?.toInt()
                    ?: 0).coerceIn(0, 999)
                latestJarakLuar = jarakLuar
                binding.tvSensor1.text = "$jarakLuar cm"
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 4. Pantau Status Penutup
        dbSampah.child("status_tutup").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val statusRaw = snapshot.getValue(String::class.java) ?: "TERTUTUP"
                // [FIX M-1] Whitelist: hanya izinkan nilai yang diketahui. Tolak input berbahaya.
                val allowedStatuses = setOf("TERBUKA", "TERTUTUP")
                val status = statusRaw.uppercase().let {
                    if (it in allowedStatuses) it else "TERTUTUP"
                }

                binding.tvStatusPenutup.text = status
                if (status == "TERBUKA") {
                    binding.tvStatusPenutup.setTextColor(colorWarning)
                } else {
                    binding.tvStatusPenutup.setTextColor(colorSafe)
                }

                if (lastStatusTutup.isNotEmpty() && lastStatusTutup != status) {
                    val pesan = if (status == "TERBUKA") {
                        "Tutup terbuka (Pengguna di jarak $latestJarakLuar cm).\nKapasitas saat ini: $latestKapasitas%."
                    } else {
                        "Tutup kembali tertutup.\nKapasitas: $latestKapasitas% (Jarak sampah: $latestJarakDalam cm)."
                    }
                    val tipe = if (status == "TERBUKA") "info" else "success"

                    simpanRiwayatKeFirebase(pesan, tipe)
                }
                lastStatusTutup = status
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun simpanRiwayatKeFirebase(pesan: String, tipe: String) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID"))
        val waktuSekarang = dateFormat.format(Calendar.getInstance().time)
        val timestamp = System.currentTimeMillis()

        val logData = mapOf(
            "pesan" to pesan,
            "waktu" to waktuSekarang,
            "tipe" to tipe,
            "timestamp" to timestamp
        )

        val logRef = FirebaseDatabase.getInstance().getReference("Logs").push()
        logRef.setValue(logData)
    }

    /**
     * Menyimpan nilai kapasitas saat ini ke Firebase berdasarkan slot waktu.
     * Slot: 0=Pagi(06-09), 1=Siang(10-13), 2=Sore(14-17), 3=Malam(18-05), 4=Kini(selalu update)
     */
    private fun simpanRekapitulasiHarian(binId: String, kapasitas: Int) {
        val calendar = Calendar.getInstance()
        val jam = calendar.get(Calendar.HOUR_OF_DAY)
        val slotIndex = when (jam) {
            in 6..9   -> 0  // Pagi
            in 10..13 -> 1  // Siang
            in 14..17 -> 2  // Sore
            else      -> 3  // Malam (18-23 dan 00-05)
        }

        val namaHari = getNamaHariFirebase(currentDayIndex)
        val basePath = if (binId == "Tempat_Sampah_1") "Tempat_Sampah_1" else "tempat_sampah/$binId"
        val dbRekap = FirebaseDatabase.getInstance()
            .getReference("$basePath/rekapitulasi_harian/$namaHari")

        // Simpan ke slot saat ini dan selalu update slot "kini" (4)
        dbRekap.child(slotIndex.toString()).setValue(kapasitas)
        dbRekap.child("4").setValue(kapasitas)
    }

    /**
     * Membaca data rekapitulasi harian dari Firebase dan mengisi grafik dengan data nyata.
     */
    private fun bacaRekapitulasiHarian(binId: String) {
        val namaHari = getNamaHariFirebase(currentDayIndex)
        val basePath = if (binId == "Tempat_Sampah_1") "Tempat_Sampah_1" else "tempat_sampah/$binId"
        val dbRekap = FirebaseDatabase.getInstance()
            .getReference("$basePath/rekapitulasi_harian/$namaHari")

        dbRekap.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var adaPerubahan = false
                for (slotIndex in 0..3) {
                    val nilai = (snapshot.child(slotIndex.toString()).getValue(Int::class.java)
                        ?: snapshot.child(slotIndex.toString()).getValue(Long::class.java)?.toInt()
                        ?: -1) // -1 = belum ada data
                    if (nilai >= 0) {
                        kumpulanData[currentDayIndex][slotIndex] = nilai.coerceIn(0, 100)
                        adaPerubahan = true
                    }
                }
                if (adaPerubahan) {
                    updateSemuaGrafik(kumpulanData[currentDayIndex], true)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    /**
     * Mengembalikan nama hari dalam format string untuk path Firebase.
     */
    private fun getNamaHariFirebase(dayIndex: Int): String {
        return when (dayIndex) {
            0 -> "senin"
            1 -> "selasa"
            2 -> "rabu"
            3 -> "kamis"
            4 -> "jumat"
            5 -> "sabtu"
            6 -> "minggu"
            else -> "senin"
        }
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

        val tinggiVisual = 12 + (persentase * 1.1).toInt()
        val layoutParams = barCard.layoutParams
        layoutParams.height = (tinggiVisual * resources.displayMetrics.density).toInt()
        barCard.layoutParams = layoutParams
    }

    companion object {
        // --- WARNA TEMA HIJAU SAMOSA ---
        private val colorBackground = Color.parseColor("#F5F7F9")
        private val colorMutedText = Color.parseColor("#888888")
        private val colorSelected = Color.parseColor("#20B273")
        private val colorWhite = Color.parseColor("#FFFFFF")

        private val colorSafe = Color.parseColor("#20B273")
        private val colorWarning = Color.parseColor("#FFA500")
        private val colorFull = Color.parseColor("#FF4B4B")
    }
}