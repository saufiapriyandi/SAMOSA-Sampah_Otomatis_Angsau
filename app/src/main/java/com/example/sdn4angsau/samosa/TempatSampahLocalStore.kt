package com.example.sdn4angsau.samosa

import android.content.Context
import android.content.SharedPreferences

class TempatSampahLocalStore(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Menyimpan data pengaturan tempat sampah ke penyimpanan lokal
     */
    fun saveTempatSampah(item: TempatSampah) {
        prefs.edit().apply {
            putString("${KEY_LOKASI}_${item.binId}", item.lokasi)
            putBoolean("${KEY_IS_ACTIVE}_${item.binId}", item.isActive)
            putInt("${KEY_PERSENTASE}_${item.binId}", item.persentase)

            // Perbaikan baris 85 & 106: Menyimpan ambang batas notifikasi asli
            putInt("${KEY_NOTIF_THRESHOLD}_${item.binId}", item.notifThreshold)
            apply()
        }
    }

    /**
     * Mengambil data tempat sampah berdasarkan Bin ID dari penyimpanan lokal
     */
    fun getTempatSampah(binId: String): TempatSampah {
        val lokasi = prefs.getString("${KEY_LOKASI}_$binId", "") ?: ""
        val isActive = prefs.getBoolean("${KEY_IS_ACTIVE}_$binId", true)
        val persentase = prefs.getInt("${KEY_PERSENTASE}_$binId", 0)

        // Membaca nilai threshold lokal, default ke 80% jika belum diatur
        val notifThreshold = prefs.getInt("${KEY_NOTIF_THRESHOLD}_$binId", 80)

        // Perbaikan Baris 116: Menggunakan Named Arguments untuk menghindari salah urutan tipe data
        return TempatSampah(
            binId = binId,
            lokasi = lokasi,
            isActive = isActive,
            persentase = persentase,
            notifThreshold = notifThreshold,
            jarakSensor = 0
        )
    }

    /**
     * Menghapus data tempat sampah tertentu dari lokal
     */
    fun deleteTempatSampah(binId: String) {
        prefs.edit().apply {
            remove("${KEY_LOKASI}_$binId")
            remove("${KEY_IS_ACTIVE}_$binId")
            remove("${KEY_PERSENTASE}_$binId")
            remove("${KEY_NOTIF_THRESHOLD}_$binId")
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "samosa_local_store"
        private const val KEY_LOKASI = "lokasi"
        private const val KEY_IS_ACTIVE = "is_active"
        private const val KEY_PERSENTASE = "persentase"
        private const val KEY_NOTIF_THRESHOLD = "notif_threshold"
    }
}