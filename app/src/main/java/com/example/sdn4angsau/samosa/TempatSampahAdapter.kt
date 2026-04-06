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

            when (item.status) {
                TempatSampahStatus.PENUH -> {
                    tvPersentase.setTextColor(Color.parseColor("#FF4B4B"))
                    tvStatusPesan.text = context.getString(R.string.dashboard_status_full)
                    tvStatusPesan.setTextColor(Color.parseColor("#FF4B4B"))
                    cardStatusPesan.setCardBackgroundColor(Color.parseColor("#1AFF4B4B"))
                    bgIconSampah.setCardBackgroundColor(Color.parseColor("#FF4B4B"))
                }
                TempatSampahStatus.WASPADA -> {
                    tvPersentase.setTextColor(Color.parseColor("#FFA500"))
                    tvStatusPesan.text = context.getString(R.string.dashboard_status_warning)
                    tvStatusPesan.setTextColor(Color.parseColor("#FFA500"))
                    cardStatusPesan.setCardBackgroundColor(Color.parseColor("#1AFFA500"))
                    bgIconSampah.setCardBackgroundColor(Color.parseColor("#FFA500"))
                }
                TempatSampahStatus.AMAN -> {
                    tvPersentase.setTextColor(Color.parseColor("#20B273"))
                    tvStatusPesan.text = context.getString(R.string.dashboard_status_safe)
                    tvStatusPesan.setTextColor(Color.parseColor("#20B273"))
                    cardStatusPesan.setCardBackgroundColor(Color.parseColor("#1A20B273"))
                    bgIconSampah.setCardBackgroundColor(Color.parseColor("#20B273"))
                }
            }

            root.setOnClickListener {
                onItemClick(item)
            }
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
