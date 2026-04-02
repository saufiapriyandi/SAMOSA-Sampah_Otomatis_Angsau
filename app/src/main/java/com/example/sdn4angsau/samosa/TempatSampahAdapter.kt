package com.example.sdn4angsau.samosa

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sdn4angsau.samosa.databinding.ItemTempatSampahBinding

class TempatSampahAdapter(private var listSampah: List<TempatSampah>) :
    RecyclerView.Adapter<TempatSampahAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemTempatSampahBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTempatSampahBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listSampah[position]

        with(holder.binding) {
            tvLokasi.text = item.lokasi
            tvBinId.text = "BIN-ID: ${item.binId}"
            tvPersentase.text = "${item.persentase}%"

            when {
                item.persentase >= 90 -> {
                    tvPersentase.setTextColor(Color.parseColor("#FF4B4B"))
                    tvStatusPesan.text = "PENUH - SEGERA KOSONGKAN"
                    tvStatusPesan.setTextColor(Color.parseColor("#FF4B4B"))
                    tvStatusPesan.setBackgroundColor(Color.parseColor("#1AFF4B4B"))
                    bgIconSampah.setCardBackgroundColor(Color.parseColor("#FF4B4B"))
                }
                item.persentase >= 60 -> {
                    tvPersentase.setTextColor(Color.parseColor("#FFA500"))
                    tvStatusPesan.text = "SEDANG - MASIH BISA"
                    tvStatusPesan.setTextColor(Color.parseColor("#FFA500"))
                    tvStatusPesan.setBackgroundColor(Color.parseColor("#1AFFA500"))
                    bgIconSampah.setCardBackgroundColor(Color.parseColor("#FFA500"))
                }
                else -> {
                    tvPersentase.setTextColor(Color.parseColor("#20B273"))
                    tvStatusPesan.text = "KOSONG - BERSIH"
                    tvStatusPesan.setTextColor(Color.parseColor("#20B273"))
                    tvStatusPesan.setBackgroundColor(Color.parseColor("#1A20B273"))
                    bgIconSampah.setCardBackgroundColor(Color.parseColor("#20B273"))
                }
            }

            // Perintah klik untuk pindah ke halaman Detail
            root.setOnClickListener {
                val intent = android.content.Intent(it.context, DetailActivity::class.java)
                intent.putExtra("EXTRA_LOKASI", item.lokasi)
                intent.putExtra("EXTRA_BINID", item.binId)
                intent.putExtra("EXTRA_PERSENTASE", item.persentase)
                it.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = listSampah.size

    // Fungsi untuk memperbarui daftar saat pencarian dilakukan
    fun updateData(newList: List<TempatSampah>) {
        listSampah = newList
        notifyDataSetChanged()
    }
}