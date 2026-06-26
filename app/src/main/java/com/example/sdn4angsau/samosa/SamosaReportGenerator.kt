package com.example.sdn4angsau.samosa

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
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

    // ── Warna tema SAMOSA ──
    private const val COLOR_GREEN_PRIMARY = 0xFF20B273.toInt()
    private const val COLOR_GREEN_DARK = 0xFF14714A.toInt()
    private const val COLOR_RED_WARNING = 0xFFE53935.toInt()
    private const val COLOR_TEXT_DARK = 0xFF222222.toInt()
    private const val COLOR_TEXT_GRAY = 0xFF888888.toInt()
    private const val COLOR_TABLE_HEADER_BG = 0xFFE8F5E9.toInt()
    private const val COLOR_TABLE_ROW_BG = 0xFFF9FBF9.toInt()
    private const val COLOR_DIVIDER = 0xFFCCCCCC.toInt()

    // ── Ukuran halaman A4 dalam satuan PostScript points (72 dpi) ──
    private const val PAGE_WIDTH = 595   // A4 width
    private const val PAGE_HEIGHT = 842  // A4 height
    private const val MARGIN = 48f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2

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

    /**
     * Mengekspor laporan SAMOSA dalam format PDF menggunakan android.graphics.pdf.PdfDocument.
     * Tidak membutuhkan library tambahan.
     *
     * File disimpan ke subdirectory reports/ di cacheDir sesuai konfigurasi file_paths.xml.
     */
    fun exportReportPdf(context: Context, period: ReportPeriod): File {
        val snapshot = buildSnapshot(context, period)
        val isDaily = period == ReportPeriod.DAILY
        val periodStr = if (isDaily) "Harian" else "Mingguan"

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // ── Paints ──
        val paintTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 22f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintSubtitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val paintSectionTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GREEN_DARK; textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintBody = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val paintBodyBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_GRAY; textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val paintTableHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GREEN_DARK; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintTableCell = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_DARK; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val paintTableCellRed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_RED_WARNING; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintTableCellGreen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GREEN_DARK; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintRect = Paint(Paint.ANTI_ALIAS_FLAG)
        val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DIVIDER; strokeWidth = 1f
        }
        val paintStatValue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GREEN_PRIMARY; textSize = 28f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintStatLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_GRAY; textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        var y = 0f

        // ══════════════════════════════════════════════
        // HEADER — Banner hijau dengan judul laporan
        // ══════════════════════════════════════════════
        val headerHeight = 110f
        paintRect.color = COLOR_GREEN_PRIMARY
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), headerHeight, paintRect)

        // Aksen garis tipis di bawah header
        paintRect.color = COLOR_GREEN_DARK
        canvas.drawRect(0f, headerHeight, PAGE_WIDTH.toFloat(), headerHeight + 4f, paintRect)

        // Teks judul
        canvas.drawText("LAPORAN ${periodStr.uppercase()}", MARGIN, 50f, paintTitle)
        canvas.drawText("SAMOSA — Sampah Otomatis Angsau", MARGIN, 72f, paintSubtitle)
        canvas.drawText("UPTD SDN 4 Angsau, Kabupaten Tapin, Kalimantan Selatan", MARGIN, 88f, paintSubtitle)
        canvas.drawText("Digenerate: ${snapshot.generatedAtLabel}", MARGIN, 104f, paintSubtitle)

        y = headerHeight + 4f + 28f

        // ══════════════════════════════════════════════
        // STATISTIK RINGKASAN — 4 kolom angka besar
        // ══════════════════════════════════════════════
        canvas.drawText("RINGKASAN", MARGIN, y, paintSectionTitle)
        y += 8f
        paintLine.color = COLOR_GREEN_PRIMARY
        paintLine.strokeWidth = 2f
        canvas.drawLine(MARGIN, y, MARGIN + 80f, y, paintLine)
        paintLine.color = COLOR_DIVIDER
        paintLine.strokeWidth = 1f
        y += 20f

        val statsData = listOf(
            Pair(snapshot.totalFullEvents.toString(), "Total Event Penuh"),
            Pair(snapshot.affectedBinsCount.toString(), "Tong Terpengaruh"),
            Pair(snapshot.topTimeLabel, "Waktu Puncak"),
            Pair(snapshot.monitoredInfoLabel, "Status Monitor")
        )

        val statColWidth = CONTENT_WIDTH / 4f
        statsData.forEachIndexed { index, (value, label) ->
            val x = MARGIN + statColWidth * index
            // Background kotak statistik
            paintRect.color = COLOR_TABLE_ROW_BG
            canvas.drawRect(x + 2f, y - 16f, x + statColWidth - 2f, y + 30f, paintRect)

            // Nilai & label
            if (value.length <= 3) {
                canvas.drawText(value, x + 10f, y + 10f, paintStatValue)
            } else {
                // Untuk teks panjang (waktu puncak, status monitor), gunakan font lebih kecil
                val smallerValue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_GREEN_PRIMARY; textSize = 13f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText(value, x + 10f, y + 6f, smallerValue)
            }
            canvas.drawText(label, x + 10f, y + 24f, paintStatLabel)
        }
        y += 50f

        // ══════════════════════════════════════════════
        // DETAIL LOKASI
        // ══════════════════════════════════════════════
        canvas.drawText("DETAIL LOKASI", MARGIN, y, paintSectionTitle)
        y += 8f
        paintLine.color = COLOR_GREEN_PRIMARY
        paintLine.strokeWidth = 2f
        canvas.drawLine(MARGIN, y, MARGIN + 100f, y, paintLine)
        paintLine.color = COLOR_DIVIDER
        paintLine.strokeWidth = 1f
        y += 18f

        canvas.drawText("Lokasi Utama: ", MARGIN, y, paintBodyBold)
        canvas.drawText(snapshot.topLocationLabel, MARGIN + paintBodyBold.measureText("Lokasi Utama: "), y, paintBody)
        y += 18f

        canvas.drawText("Waktu Puncak Penuh: ", MARGIN, y, paintBodyBold)
        canvas.drawText(snapshot.topTimeLabel, MARGIN + paintBodyBold.measureText("Waktu Puncak Penuh: "), y, paintBody)
        y += 30f

        // ══════════════════════════════════════════════
        // TABEL REKAPITULASI
        // ══════════════════════════════════════════════
        canvas.drawText("REKAPITULASI PER TONG SAMPAH", MARGIN, y, paintSectionTitle)
        y += 8f
        paintLine.color = COLOR_GREEN_PRIMARY
        paintLine.strokeWidth = 2f
        canvas.drawLine(MARGIN, y, MARGIN + 200f, y, paintLine)
        paintLine.color = COLOR_DIVIDER
        paintLine.strokeWidth = 1f
        y += 14f

        // ── Header tabel ──
        val col1W = CONTENT_WIDTH * 0.34f
        val col2W = CONTENT_WIDTH * 0.26f
        val col3W = CONTENT_WIDTH * 0.18f
        val col4W = CONTENT_WIDTH * 0.22f
        val rowHeight = 26f

        // Background header
        paintRect.color = COLOR_TABLE_HEADER_BG
        canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + rowHeight, paintRect)

        val headerY = y + 17f
        canvas.drawText("Lokasi", MARGIN + 8f, headerY, paintTableHeader)
        canvas.drawText("Bin ID", MARGIN + col1W + 8f, headerY, paintTableHeader)
        canvas.drawText("Event Penuh", MARGIN + col1W + col2W + 8f, headerY, paintTableHeader)
        canvas.drawText("Waktu Puncak", MARGIN + col1W + col2W + col3W + 8f, headerY, paintTableHeader)
        y += rowHeight

        // ── Baris data tabel ──
        snapshot.tableRows.forEachIndexed { index, row ->
            // Zebra striping
            if (index % 2 == 0) {
                paintRect.color = COLOR_TABLE_ROW_BG
                canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + rowHeight, paintRect)
            }

            val cellY = y + 17f
            canvas.drawText(row.lokasi, MARGIN + 8f, cellY, paintTableCell)
            canvas.drawText(row.binId, MARGIN + col1W + 8f, cellY, paintTableCell)
            canvas.drawText("${row.fullEventCount}x", MARGIN + col1W + col2W + 8f, cellY, paintTableCellRed)
            canvas.drawText(row.peakTimesLabel, MARGIN + col1W + col2W + col3W + 8f, cellY, paintTableCellGreen)
            y += rowHeight

            // Garis bawah baris
            canvas.drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, paintLine)
        }

        y += 30f

        // ══════════════════════════════════════════════
        // SARAN & TINDAKAN
        // ══════════════════════════════════════════════
        canvas.drawText("SARAN & TINDAKAN", MARGIN, y, paintSectionTitle)
        y += 8f
        paintLine.color = COLOR_GREEN_PRIMARY
        paintLine.strokeWidth = 2f
        canvas.drawLine(MARGIN, y, MARGIN + 130f, y, paintLine)
        paintLine.color = COLOR_DIVIDER
        paintLine.strokeWidth = 1f
        y += 18f

        // Kotak saran dengan background hijau muda
        paintRect.color = COLOR_TABLE_HEADER_BG
        canvas.drawRect(MARGIN, y - 12f, MARGIN + CONTENT_WIDTH, y + 28f, paintRect)
        // Aksen garis kiri hijau
        paintRect.color = COLOR_GREEN_PRIMARY
        canvas.drawRect(MARGIN, y - 12f, MARGIN + 4f, y + 28f, paintRect)

        canvas.drawText("💡 ${snapshot.insightText}", MARGIN + 14f, y + 4f, paintBody)
        canvas.drawText("Lakukan pengosongan secara berkala terutama sebelum jam istirahat siang.", MARGIN + 14f, y + 20f, paintBody)
        y += 50f

        // ══════════════════════════════════════════════
        // FOOTER — Catatan
        // ══════════════════════════════════════════════
        paintLine.color = COLOR_DIVIDER
        canvas.drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, paintLine)
        y += 16f
        canvas.drawText("Catatan: ${snapshot.noteText}", MARGIN, y, paintSmall)
        y += 14f
        canvas.drawText("Sistem SAMOSA — Sampah Otomatis Angsau | UPTD SDN 4 Angsau", MARGIN, y, paintSmall)
        y += 14f
        canvas.drawText("© ${Calendar.getInstance().get(Calendar.YEAR)} SAMOSA. Laporan ini bersifat rahasia dan hanya untuk keperluan internal.", MARGIN, y, paintSmall)

        // ── Aksen garis bawah halaman ──
        paintRect.color = COLOR_GREEN_PRIMARY
        canvas.drawRect(0f, PAGE_HEIGHT - 6f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), paintRect)

        document.finishPage(page)

        // ── Simpan ke file ──
        val fileName = if (isDaily) "Laporan_Harian_SAMOSA.pdf" else "Laporan_Mingguan_SAMOSA.pdf"
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()
        val file = File(reportsDir, fileName)

        FileOutputStream(file).use { outputStream ->
            document.writeTo(outputStream)
        }
        document.close()

        return file
    }
}