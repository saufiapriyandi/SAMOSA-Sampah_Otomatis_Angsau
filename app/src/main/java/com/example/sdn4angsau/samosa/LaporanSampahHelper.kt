package com.example.sdn4angsau.samosa

import java.util.Locale

// Data class pembantu untuk menstrukturkan data riwayat penuh log sampah
data class LogRiwayatSampah(
    val binId: String = "",
    val lokasi: String = "",
    val fullEventCount: Int = 0
)

class LaporanSampahHelper {

    /**
     * Memproses data tempat sampah yang aktif dan mengurutkannya berdasarkan lokasi secara eksplisit
     */
    fun prosesTempatSampahAktif(bins: List<TempatSampah>): List<TempatSampah> {
        // Perbaikan Baris 40 & 41: ganti getActive menjadi isActive dan pastikan tipe data lambda terdeteksi
        return bins.filter { bin: TempatSampah -> bin.isActive }
            .sortedWith(compareBy { bin: TempatSampah -> bin.lokasi.lowercase(Locale.getDefault()) })
    }

    /**
     * Membuat mapping laporan dari daftar data tempat sampah
     */
    fun generateRingkasanLaporan(bins: List<TempatSampah>): List<LogRiwayatSampah> {
        // Perbaikan Baris 44, 50, 51, 56: Menentukan tipe data secara gamblang agar tidak terjadi ambiguity
        return bins.mapNotNull { bin: TempatSampah ->
            if (bin.binId.isBlank()) null
            else LogRiwayatSampah(
                binId = bin.binId,
                lokasi = bin.lokasi,
                fullEventCount = if (bin.persentase >= bin.notifThreshold) 1 else 0
            )
        }.sortedWith(compareBy { log: LogRiwayatSampah -> log.lokasi.lowercase(Locale.getDefault()) })
    }

    /**
     * Menghitung total event tempat sampah penuh dari daftar riwayat laporan log
     */
    fun hitungTotalPenumpukan(logList: List<LogRiwayatSampah>): Int {
        // Perbaikan Baris 61, 62, 69, 70, 72, 73: Menghindari ambiguity fungsi bawaan Iterable Kotlin
        if (logList.isEmpty()) return 0

        return logList.sumOf { log: LogRiwayatSampah -> log.fullEventCount }
    }

    /**
     * Menghitung rata-rata tingkat penumpukan berdasarkan total log yang masuk
     */
    fun hitungRataRataPenumpukan(logList: List<LogRiwayatSampah>): Double {
        // Perbaikan Baris 78, 79, 81: Casting tipe eksplisit agar compiler tidak kebingungan
        if (logList.isEmpty()) return 0.0

        val totalMati = logList.sumOf { log: LogRiwayatSampah -> log.fullEventCount }
        return totalMati.toDouble() / logList.size.toDouble()
    }
}