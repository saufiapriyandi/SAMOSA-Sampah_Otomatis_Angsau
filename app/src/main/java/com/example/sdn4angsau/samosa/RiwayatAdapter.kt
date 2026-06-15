package com.example.sdn4angsau.samosa

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RiwayatAdapter(private val logList: List<LogItem>) : RecyclerView.Adapter<RiwayatAdapter.LogViewHolder>() {

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconTipe: ImageView = itemView.findViewById(R.id.iconLog)
        val tvAktivitas: TextView = itemView.findViewById(R.id.tvAktivitas)
        val tvDetail: TextView = itemView.findViewById(R.id.tvDetail)
        val tvJam: TextView = itemView.findViewById(R.id.tvJam)
        val tvTanggal: TextView = itemView.findViewById(R.id.tvTanggal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logList[position]

        // 1. Memecah Pesan berdasarkan Enter (\n)
        val pesanParts = log.pesan.split("\n")
        holder.tvAktivitas.text = pesanParts[0] // Baris pertama untuk Judul
        if (pesanParts.size > 1) {
            holder.tvDetail.text = pesanParts[1] // Baris kedua untuk Detail Kapasitas/Jarak
            holder.tvDetail.visibility = View.VISIBLE
        } else {
            holder.tvDetail.visibility = View.GONE
        }

        // 2. Memecah Waktu (Tanggal dan Jam)
        // Contoh format Firebase: "16/06/2026 01:17:24"
        val waktuParts = log.waktu.split(" ")
        if (waktuParts.size >= 2) {
            val tanggalRaw = waktuParts[0]
            val jamRaw = waktuParts[1]

            // Ambil Jam dan Menit saja (misal 01:17:24 menjadi 01:17)
            holder.tvJam.text = jamRaw.substringBeforeLast(":")
            holder.tvTanggal.text = tanggalRaw
        } else {
            holder.tvJam.text = log.waktu
            holder.tvTanggal.text = ""
        }

        // 3. Mengatur Warna Ikon
        when (log.tipe) {
            "success" -> holder.iconTipe.setColorFilter(Color.parseColor("#20B273")) // Hijau
            "warning" -> holder.iconTipe.setColorFilter(Color.parseColor("#FFA500")) // Orange
            "danger" -> holder.iconTipe.setColorFilter(Color.parseColor("#FF4B4B"))  // Merah
            else -> holder.iconTipe.setColorFilter(Color.parseColor("#3498DB"))      // Biru (Info)
        }
    }

    override fun getItemCount(): Int {
        return logList.size
    }
}