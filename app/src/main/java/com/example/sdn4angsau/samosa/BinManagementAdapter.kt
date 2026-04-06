package com.example.sdn4angsau.samosa

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sdn4angsau.samosa.databinding.ItemBinManagementBinding

class BinManagementAdapter(
    private val onToggleActive: (TempatSampah, Boolean) -> Unit,
    private val onEditClick: (TempatSampah) -> Unit
) : ListAdapter<TempatSampah, BinManagementAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(
        val binding: ItemBinManagementBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBinManagementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val context = holder.binding.root.context

        with(holder.binding) {
            tvManageLokasi.text = item.lokasi
            tvManageBinId.text = context.getString(R.string.management_bin_id_format, item.binId)
            tvManagePersentase.text =
                context.getString(R.string.management_percentage_format, item.persentase)
            tvManageEdit.text = context.getString(R.string.management_edit_action)

            val isActive = item.isActive
            tvManageStatus.text = if (isActive) {
                context.getString(R.string.management_status_active)
            } else {
                context.getString(R.string.management_status_inactive)
            }
            cardManageStatus.setCardBackgroundColor(
                if (isActive) Color.parseColor("#E8F5E9") else Color.parseColor("#F1F3F4")
            )
            tvManageStatus.setTextColor(
                if (isActive) Color.parseColor("#20B273") else Color.parseColor("#7A7A7A")
            )

            switchManageActive.setOnCheckedChangeListener(null)
            switchManageActive.isChecked = isActive
            switchManageActive.text = if (isActive) {
                context.getString(R.string.management_switch_active_on)
            } else {
                context.getString(R.string.management_switch_active_off)
            }
            switchManageActive.setOnCheckedChangeListener { _, checked ->
                switchManageActive.text = if (checked) {
                    context.getString(R.string.management_switch_active_on)
                } else {
                    context.getString(R.string.management_switch_active_off)
                }
                onToggleActive(item, checked)
            }

            root.setOnClickListener { onEditClick(item) }
            tvManageEdit.setOnClickListener { onEditClick(item) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<TempatSampah>() {
            override fun areItemsTheSame(oldItem: TempatSampah, newItem: TempatSampah): Boolean {
                return oldItem.binId == newItem.binId
            }

            override fun areContentsTheSame(oldItem: TempatSampah, newItem: TempatSampah): Boolean {
                return oldItem == newItem
            }
        }
    }
}
