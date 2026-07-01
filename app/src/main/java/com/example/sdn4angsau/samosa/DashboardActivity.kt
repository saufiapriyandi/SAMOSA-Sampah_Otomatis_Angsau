package com.example.sdn4angsau.samosa

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sdn4angsau.samosa.databinding.ActivityDashboardBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var sampahAdapter: TempatSampahAdapter
    private lateinit var database: DatabaseReference
    private var listSampah = mutableListOf<TempatSampah>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extraPadding = (12 * resources.displayMetrics.density).toInt()
            binding.topBar.updatePadding(top = systemBars.top + extraPadding)
            insets
        }

        window.setBackgroundDrawableResource(android.R.color.white)
        binding.fakeStatusBar.visibility = View.GONE

        // Setelah layout selesai, atur paddingTop NestedScrollView agar konten
        // tidak tertutup oleh topBar yang merupakan fixed overlay
        binding.topBar.post {
            val topBarHeight = binding.topBar.height
            binding.scrollView.updatePadding(top = topBarHeight)
        }

        sampahAdapter = TempatSampahAdapter(::openDetail)
        binding.rvTempatSampah.layoutManager = LinearLayoutManager(this)
        binding.rvTempatSampah.adapter = sampahAdapter

        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.btnRefreshDashboard.setOnClickListener {
            mulaiPantauFirebaseRealtime()
        }

        database = FirebaseDatabase.getInstance().reference
        mulaiPantauFirebaseRealtime()
    }

    private var sampahReal: TempatSampah? = null
    private var sampahManajemen: List<TempatSampah> = emptyList()

    private fun mulaiPantauFirebaseRealtime() {
        binding.progressDashboard.visibility = View.VISIBLE
        binding.rvTempatSampah.visibility = View.GONE

        // 1. Pantau Tempat Sampah Utama (IoT Hardware) di root node "Tempat_Sampah_1"
        database.child("Tempat_Sampah_1").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val kapasitas = snapshot.child("kapasitas_persen").getValue(Int::class.java)
                    ?: snapshot.child("kapasitas_persen").getValue(Long::class.java)?.toInt()
                    ?: 0

                sampahReal = TempatSampah(
                    binId = "Tempat_Sampah_1",
                    lokasi = "Tempat Sampah SAMOSA",
                    persentase = kapasitas,
                    isActive = true,
                    notifThreshold = 90
                )
                updateDashboardUI()
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressDashboard.visibility = View.GONE
                binding.cardErrorState.visibility = View.VISIBLE
                binding.tvErrorMessage.text = error.message
            }
        })

        // 2. Pantau Tempat Sampah Simulasi/Lainnya dari Manajemen (node "tempat_sampah")
        val repository = FirebaseTempatSampahRepository()
        lifecycleScope.launch {
            repository.getDaftarTempatSampahRealtime().collect { bins ->
                // Hanya ambil yang statusnya aktif
                sampahManajemen = bins.filter { it.isActive }
                updateDashboardUI()
            }
        }
    }

    private fun updateDashboardUI() {
        listSampah.clear()
        
        // Gabungkan tempat sampah utama (jika ada) dengan tempat sampah manajemen
        sampahReal?.let { listSampah.add(it) }
        
        // Urutkan tempat sampah manajemen berdasarkan lokasi
        val sortedManajemen = sampahManajemen.sortedBy { it.lokasi.lowercase(Locale.getDefault()) }
        listSampah.addAll(sortedManajemen)

        val waktuSekarang = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
            .format(Calendar.getInstance().time)
        binding.tvLastUpdate.text = "UPDATE: $waktuSekarang"

        // Update list ke RecyclerView
        sampahAdapter.submitList(listSampah.toList())

        // ── PERBAIKAN NOTIFIKASI ──────────────────────────────────
        // syncNotifications() harus dipanggil setiap kali data Firebase
        // berubah agar notifikasi "sampah penuh" benar-benar dikirim ke HP.
        TempatSampahNotificationHelper.syncNotifications(
            context = this@DashboardActivity,
            bins = listSampah.toList()
        )
        // ─────────────────────────────────────────────────────────

        val total = listSampah.size
        val penuh = listSampah.count { it.persentase >= 60 }
        val aman = total - penuh

        binding.tvSummaryTotalValue.text = total.toString()
        binding.tvSummaryFullValue.text = penuh.toString()
        binding.tvSummarySafeValue.text = aman.toString()

        if (penuh > 0) {
            binding.cardWarning.visibility = View.VISIBLE
            binding.tvWarningMessage.text = "Terdapat $penuh tempat sampah yang memerlukan perhatian segera!"
        } else {
            binding.cardWarning.visibility = View.GONE
        }

        binding.progressDashboard.visibility = View.GONE
        binding.rvTempatSampah.visibility = View.VISIBLE
        binding.cardErrorState.visibility = View.GONE
    }

    private fun openDetail(item: TempatSampah) {
        val intent = Intent(this, DetailActivity::class.java).apply {
            putExtra("EXTRA_LOKASI", item.lokasi)
            putExtra("EXTRA_BINID", item.binId)
            putExtra("EXTRA_PERSENTASE", item.persentase)
        }
        startActivity(intent)
    }
}
