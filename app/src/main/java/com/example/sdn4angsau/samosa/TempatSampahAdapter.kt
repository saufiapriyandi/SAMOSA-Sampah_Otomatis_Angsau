package com.example.sdn4angsau.samosa

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sdn4angsau.samosa.databinding.ItemTempatSampahBinding

class TempatSampahAdapter(
    private val onItemClick: (TempatSampah) -> Unit
) : ListAdapter<TempatSampah, TempatSampahAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: ItemTempatSampahBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTempatSampahBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val context = holder.binding.root.context

        with(holder.binding) {
            tvLokasi.text = item.lokasi
            tvBinId.text = context.getString(R.string.dashboard_bin_id_format, item.binId)
            tvPersentase.text = "${item.persentase}%"
            val uiColors = colorsForStatus(item.status)

            tvPersentase.setTextColor(uiColors.accentColor)
            tvStatusPesan.setTextColor(uiColors.accentColor)
            cardStatusPesan.setCardBackgroundColor(uiColors.surfaceColor)
            bgIconSampah.setCardBackgroundColor(uiColors.accentColor)

            tvStatusPesan.text = when (item.status) {
                TempatSampahStatus.PENUH -> context.getString(R.string.dashboard_status_full)
                TempatSampahStatus.WASPADA -> context.getString(R.string.dashboard_status_warning)
                TempatSampahStatus.AMAN -> context.getString(R.string.dashboard_status_safe)
            }

            root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    companion object {
        private val fullAccent = Color.parseColor("#FF4B4B")
        private val fullSurface = Color.parseColor("#1AFF4B4B")
        private val warningAccent = Color.parseColor("#FFA500")
        private val warningSurface = Color.parseColor("#1AFFA500")
        private val safeAccent = Color.parseColor("#20B273")
        private val safeSurface = Color.parseColor("#1A20B273")

        private val DiffCallback = object : DiffUtil.ItemCallback<TempatSampah>() {
            override fun areItemsTheSame(oldItem: TempatSampah, newItem: TempatSampah): Boolean {
                return oldItem.binId == newItem.binId
            }

            override fun areContentsTheSame(oldItem: TempatSampah, newItem: TempatSampah): Boolean {
                return oldItem == newItem
            }
        }

        private fun colorsForStatus(status: TempatSampahStatus): StatusUiColors {
            return when (status) {
                TempatSampahStatus.PENUH -> StatusUiColors(fullAccent, fullSurface)
                TempatSampahStatus.WASPADA -> StatusUiColors(warningAccent, warningSurface)
                TempatSampahStatus.AMAN -> StatusUiColors(safeAccent, safeSurface)
            }
        }
    }

    private data class StatusUiColors(
        val accentColor: Int,
        val surfaceColor: Int
    )
}
