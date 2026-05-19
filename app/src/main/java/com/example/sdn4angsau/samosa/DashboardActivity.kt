package com.example.sdn4angsau.samosa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.NestedScrollView
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

    // Variabel penampung untuk menggabungkan data asli dan data dummy
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
        enableEdgeToEdge()

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            val extraPaddingTop = (16 * resources.displayMetrics.density).toInt()
            binding.topBar.setPadding(binding.topBar.paddingLeft, systemBars.top + extraPaddingTop, binding.topBar.paddingRight, binding.topBar.paddingBottom)

            val layoutParams = binding.fakeStatusBar.layoutParams
            layoutParams.height = systemBars.top
            binding.fakeStatusBar.layoutParams = layoutParams

            binding.scrollView.setPadding(binding.scrollView.paddingLeft, binding.scrollView.paddingTop, binding.scrollView.paddingRight, systemBars.bottom)

            insets
        }

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            val alpha = (scrollY / 150f).coerceIn(0f, 1f)
            binding.fakeStatusBar.alpha = alpha

            if (alpha == 1f) {
                binding.fakeStatusBar.elevation = 8f
            } else {
                binding.fakeStatusBar.elevation = 0f
            }
        })

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

    // Menggabungkan data Firebase dengan data dari Repository
    private fun perbaruiTampilanList() {
        val listGabungan = mutableListOf<TempatSampah>()

        // Memasukkan data asli ke urutan pertama (jika ada)
        dataFirebaseAsli?.let { listGabungan.add(it) }

        // Memasukkan sisa data dummy
        listGabungan.addAll(dataDummySaatIni)

        sampahAdapter.submitList(listGabungan)
    }

    private fun setupFirebaseRealtimeLog() {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("Tempat_Sampah_1")

        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val statusTutup = snapshot.child("status_tutup").getValue(String::class.java) ?: "Tidak Terbaca"
                    val jarakLuar = snapshot.child("jarak_cm").getValue(Int::class.java) ?: 0
                    val kapasitasDalam = snapshot.child("kapasitas_persen").getValue(Int::class.java) ?: 0

                    Log.d("SAMOSA_IOT", "======= UPDATE DATA IOT SAMOSA =======")
                    Log.d("SAMOSA_IOT", "Sensor Jarak Luar  : $jarakLuar cm")
                    Log.d("SAMOSA_IOT", "Motor Servo Tutup  : $statusTutup")
                    Log.d("SAMOSA_IOT", "Sensor Dalam (Isi) : $kapasitasDalam %")
                    Log.d("SAMOSA_IOT", "======================================")

                    // Bungkus data menjadi objek TempatSampah tanpa isFull
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

            binding.cardWarning.visibility =
                if (!state.isLoading && state.errorMessage == null && state.showWarning) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            binding.tvWarningTitle.text = getString(R.string.dashboard_warning_title)
            binding.tvWarningMessage.text = buildWarningMessage(state.warningBins)

            binding.cardDataStale.visibility =
                if (!state.isLoading && state.errorMessage == null && state.isDataStale) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            binding.tvDataStaleMessage.text =
                getString(R.string.dashboard_stale_message, state.staleMinutes)

            binding.tvEmptyState.text = when (state.emptyState) {
                DashboardEmptyState.NO_ACTIVE_BINS ->
                    getString(R.string.dashboard_empty_no_active_bins)
                DashboardEmptyState.SEARCH_NO_RESULT ->
                    getString(R.string.dashboard_empty_search)
                DashboardEmptyState.NONE -> ""
            }
            binding.tvEmptyState.visibility = if (
                state.emptyState != DashboardEmptyState.NONE &&
                state.errorMessage == null &&
                !state.isLoading
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

            binding.rvTempatSampah.visibility =
                if (state.errorMessage == null && !state.isLoading && state.visibleBins.isNotEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            // Simpan data state ke variabel, lalu perbarui tampilan
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

        val notificationPermissionGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

        if (!notificationPermissionGranted) {
            if (!hasRequestedNotificationPermission) {
                hasRequestedNotificationPermission = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            return
        }

        TempatSampahNotificationHelper.syncNotifications(this, configuredBins)
    }

    private fun buildWarningMessage(fullBins: List<TempatSampah>): String {
        if (fullBins.isEmpty()) {
            return getString(R.string.dashboard_warning_placeholder)
        }

        return if (fullBins.size == 1) {
            getString(
                R.string.dashboard_warning_message_single,
                fullBins.first().lokasi,
                fullBins.first().persentase
            )
        } else {
            getString(
                R.string.dashboard_warning_message_multiple,
                fullBins.size,
                fullBins.first().lokasi
            )
        }
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