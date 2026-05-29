package com.example.sdn4angsau.samosa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sdn4angsau.samosa.databinding.ActivityDashboardBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var sampahAdapter: TempatSampahAdapter
    private var hasResumedOnce = false

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModel.Factory(MockTempatSampahRepository(applicationContext))
    }

    private var lastHandledDataVersion: Long = 0L
    private var latestBinsForNotification: List<TempatSampah> = emptyList()
    private var hasRequestedNotificationPermission = false

    private var dataFirebaseAsli: TempatSampah? = null
    private var dataDummySaatIni: List<TempatSampah> = emptyList()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            TempatSampahNotificationHelper.syncNotifications(this, latestBinsForNotification)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Mematikan Edge-to-Edge sementara untuk memperbaiki masalah layar hitam di API 35/36
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Memastikan latar belakang jendela solid putih
        window.setBackgroundDrawableResource(android.R.color.white)

        // Menyembunyikan komponen status bar buatan yang mungkin menyebabkan glitch layout
        binding.fakeStatusBar.visibility = View.GONE

        sampahAdapter = TempatSampahAdapter(::openDetail)
        binding.rvTempatSampah.layoutManager = LinearLayoutManager(this)
        binding.rvTempatSampah.adapter = sampahAdapter

        binding.btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.btnRetryDashboard.setOnClickListener {
            viewModel.loadData()
        }

        binding.btnRefreshDashboard.setOnClickListener {
            viewModel.loadData()
        }

        binding.etSearch.doAfterTextChanged { editable ->
            viewModel.updateSearchQuery(editable?.toString().orEmpty())
        }

        observeUiState()
        setupFirebaseRealtimeLog()
    }

    private fun perbaruiTampilanList() {
        val listGabungan = mutableListOf<TempatSampah>()
        dataFirebaseAsli?.let { listGabungan.add(it) }
        listGabungan.addAll(dataDummySaatIni)
        sampahAdapter.submitList(listGabungan)
    }

    private fun setupFirebaseRealtimeLog() {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("Tempat_Sampah_1")

        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val kapasitasDalam = snapshot.child("kapasitas_persen").getValue(Int::class.java) ?: 0
                    dataFirebaseAsli = TempatSampah(
                        binId = "Tempat_Sampah_1",
                        lokasi = "Alat SAMOSA Asli",
                        persentase = kapasitasDalam
                    )
                    perbaruiTampilanList()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SAMOSA_IOT", "Gagal terhubung ke Firebase: ${error.message}")
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (hasResumedOnce) {
            viewModel.loadData()
        } else {
            hasResumedOnce = true
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(this) { state ->
            binding.progressDashboard.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.btnRefreshDashboard.isEnabled = !state.isLoading

            binding.cardErrorState.visibility = if (state.errorMessage != null) View.VISIBLE else View.GONE
            binding.tvErrorMessage.text = state.errorMessage ?: getString(R.string.dashboard_error_message_default)

            binding.tvSummaryTotalValue.text = state.totalCount.toString()
            binding.tvSummaryFullValue.text = state.fullCount.toString()
            binding.tvSummarySafeValue.text = state.safeCount.toString()

            binding.tvLastUpdate.text = if (state.lastUpdatedLabel.isBlank()) {
                getString(R.string.dashboard_update_placeholder)
            } else {
                getString(R.string.dashboard_update_format, state.lastUpdatedLabel)
            }

            binding.cardWarning.visibility = if (!state.isLoading && state.errorMessage == null && state.showWarning) View.VISIBLE else View.GONE
            binding.tvEmptyState.visibility = if (state.emptyState != DashboardEmptyState.NONE && state.errorMessage == null && !state.isLoading) View.VISIBLE else View.GONE
            binding.rvTempatSampah.visibility = if (state.errorMessage == null && !state.isLoading && state.visibleBins.isNotEmpty()) View.VISIBLE else View.GONE

            dataDummySaatIni = state.visibleBins
            perbaruiTampilanList()

            latestBinsForNotification = TempatSampahLocalStore.getAll(this)
            if (state.dataVersion != 0L && state.dataVersion != lastHandledDataVersion) {
                lastHandledDataVersion = state.dataVersion
                handleNotifications(state.allBins)
            }
        }
    }

    private fun handleNotifications(bins: List<TempatSampah>) {
        val configuredBins = TempatSampahLocalStore.getAll(this)
        if (configuredBins.isEmpty()) return
        val hasFullBins = bins.any { it.isFull }
        if (!hasFullBins) {
            TempatSampahNotificationHelper.syncNotifications(this, configuredBins)
            return
        }
        val notificationPermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!notificationPermissionGranted) {
            if (!hasRequestedNotificationPermission) {
                hasRequestedNotificationPermission = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            return
        }
        TempatSampahNotificationHelper.syncNotifications(this, configuredBins)
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
