package com.example.sdn4angsau.samosa

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sdn4angsau.samosa.databinding.ActivityBinManagementBinding
import com.example.sdn4angsau.samosa.databinding.DialogBinFormBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

class BinManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBinManagementBinding
    private lateinit var managementAdapter: BinManagementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityBinManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        managementAdapter = BinManagementAdapter(
            onToggleActive = ::updateBinActiveState,
            onEditClick = ::showBinFormDialog
        )

        binding.rvManageBins.layoutManager = LinearLayoutManager(this)
        binding.rvManageBins.adapter = managementAdapter

        binding.btnBackManageBins.setOnClickListener { finish() }
        binding.btnAddBin.setOnClickListener { showBinFormDialog(null) }

        loadBins()
    }

    override fun onResume() {
        super.onResume()
        loadBins()
    }

    private fun loadBins() {
        val bins = TempatSampahLocalStore.getAll(this)
            .sortedWith(
                compareByDescending<TempatSampah> { it.isActive }
                    .thenBy { it.lokasi.lowercase(Locale.getDefault()) }
            )

        managementAdapter.submitList(bins)
        binding.tvManageEmpty.visibility = if (bins.isEmpty()) View.VISIBLE else View.GONE
        binding.rvManageBins.visibility = if (bins.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun updateBinActiveState(item: TempatSampah, isActive: Boolean) {
        if (item.isActive == isActive) return

        TempatSampahLocalStore.updateActive(this, item.binId, isActive)
        loadBins()

        val message = if (isActive) {
            getString(R.string.management_toast_activated, item.lokasi)
        } else {
            getString(R.string.management_toast_inactivated, item.lokasi)
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showBinFormDialog(existingItem: TempatSampah?) {
        val dialogBinding = DialogBinFormBinding.inflate(layoutInflater)

        if (existingItem != null) {
            dialogBinding.etLokasiBinForm.setText(existingItem.lokasi)
            dialogBinding.etBinIdBinForm.setText(existingItem.binId)
            dialogBinding.etPersentaseBinForm.setText(existingItem.persentase.toString())
            dialogBinding.switchAktifBinForm.isChecked = existingItem.isActive
        } else {
            dialogBinding.switchAktifBinForm.isChecked = true
        }

        val dialog = MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_SAMOSA_MaterialAlertDialog)
            .setTitle(
                if (existingItem == null) {
                    getString(R.string.management_dialog_add_title)
                } else {
                    getString(R.string.management_dialog_edit_title, existingItem.lokasi)
                }
            )
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.management_cancel_button, null)
            .setPositiveButton(R.string.management_save_button, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.text_gray))
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.green_primary))
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val lokasi = dialogBinding.etLokasiBinForm.text.toString().trim()
                val binId = dialogBinding.etBinIdBinForm.text.toString().trim()
                val persentase = dialogBinding.etPersentaseBinForm.text.toString().trim().toIntOrNull()

                dialogBinding.tilLokasiBinForm.error = null
                dialogBinding.tilBinIdBinForm.error = null
                dialogBinding.tilPersentaseBinForm.error = null

                when {
                    lokasi.isBlank() -> {
                        dialogBinding.tilLokasiBinForm.error =
                            getString(R.string.management_validation_location)
                    }
                    binId.isBlank() -> {
                        dialogBinding.tilBinIdBinForm.error =
                            getString(R.string.management_validation_bin_id)
                    }
                    !TempatSampahLocalStore.isBinIdAvailable(this, binId, existingItem?.binId) -> {
                        dialogBinding.tilBinIdBinForm.error =
                            getString(R.string.management_validation_bin_id_duplicate)
                    }
                    persentase == null || persentase !in 0..100 -> {
                        dialogBinding.tilPersentaseBinForm.error =
                            getString(R.string.management_validation_percentage)
                    }
                    else -> {
                        val item = TempatSampah(
                            binId = binId,
                            lokasi = lokasi,
                            persentase = persentase,
                            isActive = dialogBinding.switchAktifBinForm.isChecked
                        )

                        TempatSampahLocalStore.upsert(this, item, existingItem?.binId)
                        loadBins()

                        Toast.makeText(
                            this,
                            getString(
                                if (existingItem == null) {
                                    R.string.management_toast_saved_new
                                } else {
                                    R.string.management_toast_saved_edit
                                }
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }
}
