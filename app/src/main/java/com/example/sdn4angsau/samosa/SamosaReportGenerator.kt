package com.example.sdn4angsau.samosa

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// 1. Enum untuk Pilihan Waktu
enum class ReportPeriod {
    DAILY, WEEKLY
}

// 2. Struktur Data untuk Baris Tabel
data class ReportTableRow(
    val lokasi: String,
    val binId: String,
    val fullEventCount: Int,
    val peakTimesLabel: String
)

// 3. Struktur Data untuk Laporan Utuh
data class ReportSnapshot(
    val periodLabel: String,
    val generatedAtLabel: String,
    val totalFullEvents: Int,
    val affectedBinsCount: Int,
    val topLocationLabel: String,
    val topTimeLabel: String,
    val monitoredInfoLabel: String,
    val insightText: String,
    val noteText: String,
    val tableRows: List<ReportTableRow>
)

// 4. Generator Data Laporan Otomatis
object SamosaReportGenerator {

    fun buildSnapshot(context: Context, period: ReportPeriod): ReportSnapshot {
        val isDaily = period == ReportPeriod.DAILY
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))
        val dateStr = dateFormat.format(Calendar.getInstance().time)

        return ReportSnapshot(
            periodLabel = if (isDaily) "Harian" else "Mingguan",
            generatedAtLabel = dateStr,
            totalFullEvents = if (isDaily) 2 else 10,
            affectedBinsCount = 1,
            topLocationLabel = "Tempat Sampah SAMOSA",
            topTimeLabel = "12:00 - 14:00",
            monitoredInfoLabel = "1 Aktif, 0 Nonaktif",
            insightText = "Saran: Lakukan pengosongan sebelum jam istirahat siang.",
            noteText = "Laporan ini digenerate secara otomatis oleh sistem SAMOSA.",
            tableRows = listOf(
                ReportTableRow(
                    lokasi = "Tempat Sampah SAMOSA",
                    binId = "Tempat_Sampah_1",
                    fullEventCount = if (isDaily) 2 else 10,
                    peakTimesLabel = "12:00, 14:00"
                )
            )
        )
    }

    fun buildReport(context: Context, period: ReportPeriod): String {
        val isDaily = period == ReportPeriod.DAILY
        val periodStr = if (isDaily) "Harian" else "Mingguan"

        return """
            LAPORAN ${periodStr.uppercase()} SAMOSA
            Lokasi: SDN 4 Angsau
            Waktu Cetak: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Calendar.getInstance().time)}
            
            -----------------------------------------
            REKAPITULASI:
            - Tong Sampah SAMOSA (Tempat_Sampah_1) terpantau penuh sebanyak ${if (isDaily) 2 else 10} kali.
            - Waktu Puncak (Sering Penuh): 12:00 & 14:00
            
            TINDAKAN DISARANKAN:
            Lakukan pengosongan secara berkala terutama sebelum atau pada saat jam istirahat siang agar tumpukan tidak berlebih.
            -----------------------------------------
        """.trimIndent()
    }

    fun exportReportFile(context: Context, period: ReportPeriod, content: String): File {
        val fileName = if (period == ReportPeriod.DAILY) "Laporan_Harian_SAMOSA.txt" else "Laporan_Mingguan_SAMOSA.txt"
        val file = File(context.cacheDir, fileName)
        file.writeText(content)
        return file
    }
}