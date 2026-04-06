package com.example.sdn4angsau.samosa

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

object TempatSampahLocalStore {

    private const val PREF_NAME = "SamosaLocalBins"
    private const val KEY_BINS = "bins_json"
    private const val KEY_BIN_ID = "bin_id"
    private const val KEY_LOKASI = "lokasi"
    private const val KEY_PERSENTASE = "persentase"
    private const val KEY_IS_ACTIVE = "is_active"

    fun getAll(context: Context): MutableList<TempatSampah> {
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_BINS, null)

        if (raw.isNullOrBlank()) {
            val defaults = defaultBins()
            saveAll(context, defaults)
            return defaults.toMutableList()
        }

        return runCatching { parseBins(raw) }.getOrElse {
            val defaults = defaultBins()
            saveAll(context, defaults)
            defaults.toMutableList()
        }
    }

    fun getActive(context: Context): List<TempatSampah> {
        return getAll(context).filter { it.isActive }
    }

    fun findById(context: Context, binId: String): TempatSampah? {
        return getAll(context).firstOrNull { it.binId.equals(binId, ignoreCase = true) }
    }

    fun upsert(context: Context, item: TempatSampah, originalBinId: String? = null) {
        val bins = getAll(context)
        val targetBinId = originalBinId ?: item.binId
        val index = bins.indexOfFirst { it.binId.equals(targetBinId, ignoreCase = true) }

        if (index >= 0) {
            bins[index] = item
        } else {
            bins.add(item)
        }

        saveAll(context, bins.sortedBy { it.lokasi.lowercase(Locale.getDefault()) })
    }

    fun updateActive(context: Context, binId: String, isActive: Boolean) {
        val bins = getAll(context)
        val index = bins.indexOfFirst { it.binId.equals(binId, ignoreCase = true) }
        if (index < 0) return

        bins[index] = bins[index].copy(isActive = isActive)
        saveAll(context, bins)
    }

    fun isBinIdAvailable(context: Context, candidateBinId: String, originalBinId: String? = null): Boolean {
        return getAll(context).none {
            it.binId.equals(candidateBinId, ignoreCase = true) &&
                !it.binId.equals(originalBinId, ignoreCase = true)
        }
    }

    private fun saveAll(context: Context, bins: List<TempatSampah>) {
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()

        bins.forEach { item ->
            jsonArray.put(
                JSONObject()
                    .put(KEY_BIN_ID, item.binId)
                    .put(KEY_LOKASI, item.lokasi)
                    .put(KEY_PERSENTASE, item.persentase)
                    .put(KEY_IS_ACTIVE, item.isActive)
            )
        }

        prefs.edit {
            putString(KEY_BINS, jsonArray.toString())
        }
    }

    private fun parseBins(raw: String): MutableList<TempatSampah> {
        val jsonArray = JSONArray(raw)
        val bins = mutableListOf<TempatSampah>()

        for (index in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(index)
            bins.add(
                TempatSampah(
                    binId = jsonObject.optString(KEY_BIN_ID),
                    lokasi = jsonObject.optString(KEY_LOKASI),
                    persentase = jsonObject.optInt(KEY_PERSENTASE),
                    isActive = jsonObject.optBoolean(KEY_IS_ACTIVE, true)
                )
            )
        }

        return bins
    }

    private fun defaultBins(): List<TempatSampah> {
        return listOf(
            TempatSampah("3", "Laboratorium", 100, true),
            TempatSampah("4", "Ruang Kantor", 95, true),
            TempatSampah("5", "Kantin SDN 4", 78, true),
            TempatSampah("1", "Perpustakaan", 45, true),
            TempatSampah("2", "Ruang Guru", 12, true)
        )
    }
}
