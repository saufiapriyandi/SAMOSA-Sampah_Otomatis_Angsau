package com.example.sdn4angsau.samosa

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sdn4angsau.samosa.databinding.ActivityBinManagementBinding
import com.example.sdn4angsau.samosa.databinding.DialogBinFormBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.min

class BinManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBinManagementBinding
    private lateinit var managementAdapter: BinManagementAdapter
    private val repository: TempatSampahRepository = FirebaseTempatSampahRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityBinManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Penyesuaian Header agar sama persis dengan Profile (Padding Status Bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extraTopPadding = (12 * resources.displayMetrics.density).toInt()
            binding.headerManageBar.updatePadding(top = systemBars.top + extraTopPadding)
            insets
        }

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        managementAdapter = BinManagementAdapter(
            onToggleActive = ::updateBinActiveState,
            onEditClick = ::showBinFormDialog
        )

        binding.rvManageBins.layoutManager = LinearLayoutManager(this)
        binding.rvManageBins.adapter = managementAdapter

        binding.btnBackManageBins.setOnClickListener { finish() }
        binding.btnAddBin.setOnClickListener { showBinFormDialog(null) }

        observeBins()
    }

    private fun observeBins() {
        lifecycleScope.launch {
            repository.getDaftarTempatSampahRealtime()
                .catch { e ->
                    Toast.makeText(this@BinManagementActivity, "Gagal memuat data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                .collect { bins ->
                    val sortedBins = bins.sortedWith(
                        compareByDescending<TempatSampah> { it.isActive }
                            .thenBy { it.lokasi.lowercase(Locale.getDefault()) }
                    )

                    // PERBAIKAN BARIS 76: Menggunakan instance store lokal untuk menyimpan seluruh item list sebagai cache
                    val localStore = TempatSampahLocalStore(this@BinManagementActivity)
                    sortedBins.forEach { bin ->
                        localStore.saveTempatSampah(bin)
                    }

                    managementAdapter.submitList(sortedBins)
                    binding.tvManageEmpty.visibility = if (sortedBins.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvManageBins.visibility = if (sortedBins.isEmpty()) View.GONE else View.VISIBLE
                }
        }
    }

    private fun updateBinActiveState(item: TempatSampah, isActive: Boolean) {
        if (item.isActive == isActive) return
        saveToFirebase(item.copy(isActive = isActive))

        val message = if (isActive) {
            getString(R.string.management_toast_activated, item.lokasi)
        } else {
            getString(R.string.management_toast_inactivated, item.lokasi)
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showBinFormDialog(existingItem: TempatSampah?) {
        val dialogBinding = DialogBinFormBinding.inflate(layoutInflater)
        val dialogTitle = if (existingItem == null) {
            getString(R.string.management_dialog_add_title)
        } else {
            getString(R.string.management_dialog_edit_title, existingItem.lokasi)
        }

        dialogBinding.tvBinFormTitle.text = dialogTitle

        if (existingItem != null) {
            dialogBinding.etLokasiBinForm.setText(existingItem.lokasi)
            dialogBinding.etBinIdBinForm.setText(existingItem.binId)
            dialogBinding.etPersentaseBinForm.setText(existingItem.persentase.toString())
            dialogBinding.etThresholdBinForm.setText(existingItem.notifThreshold.toString())
            dialogBinding.switchAktifBinForm.isChecked = existingItem.isActive
        } else {
            dialogBinding.etThresholdBinForm.setText("90")
            dialogBinding.switchAktifBinForm.isChecked = true
        }

        val dialog = MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_SAMOSA_MaterialAlertDialog)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.management_cancel_button, null)
            .setPositiveButton(R.string.management_save_button, null)
            .create()

        dialog.setOnShowListener {
            val dialogWidth = min(
                (resources.displayMetrics.widthPixels * 0.8f).toInt(),
                (332 * resources.displayMetrics.density).toInt()
            )

            dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_white)
            dialog.window?.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.text_gray))
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.green_primary))
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val lokasi = dialogBinding.etLokasiBinForm.text.toString().trim()
                val binId = dialogBinding.etBinIdBinForm.text.toString().trim()
                val persentase = dialogBinding.etPersentaseBinForm.text.toString().trim().toIntOrNull()
                val threshold = dialogBinding.etThresholdBinForm.text.toString().trim().toIntOrNull()

                dialogBinding.tilLokasiBinForm.error = null
                dialogBinding.tilBinIdBinForm.error = null
                dialogBinding.tilPersentaseBinForm.error = null
                dialogBinding.tilThresholdBinForm.error = null

                when {
                    lokasi.isBlank() -> {
                        dialogBinding.tilLokasiBinForm.error = getString(R.string.management_validation_location)
                    }
                    binId.isBlank() -> {
                        dialogBinding.tilBinIdBinForm.error = getString(R.string.management_validation_bin_id)
                    }
                    persentase == null || persentase !in 0..100 -> {
                        dialogBinding.tilPersentaseBinForm.error = getString(R.string.management_validation_percentage)
                    }
                    threshold == null || threshold !in 0..100 -> {
                        dialogBinding.tilThresholdBinForm.error = getString(R.string.management_validation_threshold)
                    }
                    else -> {
                        val item = TempatSampah(
                            binId = binId,
                            lokasi = lokasi,
                            isActive = dialogBinding.switchAktifBinForm.isChecked,
                            persentase = persentase,
                            notifThreshold = threshold,
                            jarakSensor = 0
                        )

                        saveToFirebase(item)

                        Toast.makeText(
                            this,
                            getString(if (existingItem == null) R.string.management_toast_saved_new else R.string.management_toast_saved_edit),
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun saveToFirebase(item: TempatSampah) {
        val database = FirebaseDatabase.getInstance("https://samosa-sampah-otomatis-angsau-default-rtdb.asia-southeast1.firebasedatabase.app/")
        val binRef = database.getReference("tempat_sampah").child(item.binId)

        val data = mapOf(
            "binId" to item.binId,
            "lokasi" to item.lokasi,
            "isActive" to item.isActive,
            "persentase" to item.persentase,
            "notifThreshold" to item.notifThreshold
        )

        binRef.setValue(data).addOnFailureListener {
            Toast.makeText(this, "Gagal sinkronisasi ke Firebase: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}