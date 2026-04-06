package com.example.sdn4angsau.samosa

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class ReportPeriod {
    DAILY,
    WEEKLY
}

data class ReportSnapshot(
    val period: ReportPeriod,
    val periodLabel: String,
    val generatedAtLabel: String,
    val totalFullEvents: Int,
    val affectedBinsCount: Int,
    val topLocationLabel: String,
    val topTimeLabel: String,
    val monitoredInfoLabel: String,
    val insightText: String,
    val tableRows: List<ReportTableRow>,
    val noteText: String
)

data class ReportTableRow(
    val lokasi: String,
    val binId: String,
    val fullEventCount: Int,
    val peakTimesLabel: String
)

object LaporanSampahHelper {

    private val localeId = Locale("id", "ID")

    fun buildSnapshot(context: Context, period: ReportPeriod): ReportSnapshot {
        val activeBins = TempatSampahLocalStore.getActive(context)
            .sortedBy { it.lokasi.lowercase(localeId) }
        val now = Calendar.getInstance()
        val currentDayIndex = TempatSampahHistoryHelper.getCurrentSchoolDayIndex(now)
        val tableRows = activeBins.mapNotNull { bin ->
            val events = TempatSampahHistoryHelper.getFullEvents(bin, period)
            if (events.isEmpty()) {
                null
            } else {
                ReportTableRow(
                    lokasi = bin.lokasi,
                    binId = bin.binId,
                    fullEventCount = events.size,
                    peakTimesLabel = TempatSampahHistoryHelper.buildPeakTimesLabel(events)
                )
            }
        }.sortedWith(
            compareByDescending<ReportTableRow> { it.fullEventCount }
                .thenBy { it.lokasi.lowercase(localeId) }
        )

        val allEvents = activeBins.flatMap { TempatSampahHistoryHelper.getFullEvents(it, period) }
        val topLocation = tableRows.firstOrNull()?.lokasi ?: "-"
        val topTime = TempatSampahHistoryHelper.buildTopTimeLabel(allEvents)

        return ReportSnapshot(
            period = period,
            periodLabel = buildPeriodLabel(period, now),
            generatedAtLabel = SimpleDateFormat("dd MMMM yyyy, HH:mm", localeId).format(now.time),
            totalFullEvents = tableRows.sumOf { it.fullEventCount },
            affectedBinsCount = tableRows.size,
            topLocationLabel = topLocation,
            topTimeLabel = if (allEvents.isEmpty()) context.getString(R.string.report_peak_none) else topTime,
            monitoredInfoLabel = context.getString(R.string.report_monitored_info, activeBins.size),
            insightText = buildInsightText(
                context = context,
                period = period,
                dayLabel = TempatSampahHistoryHelper.getDayLabel(currentDayIndex),
                totalFullEvents = tableRows.sumOf { it.fullEventCount },
                affectedBinsCount = tableRows.size,
                topLocation = topLocation,
                topTime = if (allEvents.isEmpty()) context.getString(R.string.report_peak_none) else topTime
            ),
            tableRows = tableRows,
            noteText = buildNoteText(context, period)
        )
    }

    fun buildReport(context: Context, period: ReportPeriod): String {
        val snapshot = buildSnapshot(context, period)

        val lines = mutableListOf<String>()
        lines += if (period == ReportPeriod.DAILY) {
            context.getString(R.string.report_export_title_daily)
        } else {
            context.getString(R.string.report_export_title_weekly)
        }
        lines += "Periode: ${snapshot.periodLabel}"
        lines += "Dibuat: ${snapshot.generatedAtLabel}"
        lines += ""
        lines += "Ringkasan"
        lines += "- Kejadian penuh: ${snapshot.totalFullEvents}"
        lines += "- Tong terdampak: ${snapshot.affectedBinsCount}"
        lines += "- Ruangan paling rawan: ${snapshot.topLocationLabel}"
        lines += "- Jam paling rawan: ${snapshot.topTimeLabel}"
        lines += ""
        lines += "Temuan utama"
        lines += "- ${snapshot.insightText}"

        lines += ""
        lines += "Tong yang sering penuh"
        if (snapshot.tableRows.isEmpty()) {
            lines += "- Belum ada tong yang sering penuh pada periode ini."
        } else {
            snapshot.tableRows.forEach { row ->
                lines += "- ${row.lokasi} (${row.binId}) | ${row.fullEventCount} kejadian | ${row.peakTimesLabel}"
            }
        }

        lines += ""
        lines += "Catatan"
        snapshot.noteText.lines().forEach { line ->
            lines += "- $line"
        }

        return lines.joinToString("\n")
    }

    fun exportReportFile(context: Context, period: ReportPeriod, content: String): File {
        val reportDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", localeId).format(Calendar.getInstance().time)
        val prefix = if (period == ReportPeriod.DAILY) "laporan_harian" else "laporan_mingguan"
        return File(reportDir, "${prefix}_$timestamp.txt").apply {
            writeText(content)
        }
    }

    private fun buildPeriodLabel(period: ReportPeriod, calendar: Calendar): String {
        return if (period == ReportPeriod.DAILY) {
            SimpleDateFormat("EEEE, dd MMMM yyyy", localeId).format(calendar.time)
        } else {
            val start = calendar.clone() as Calendar
            start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val end = start.clone() as Calendar
            end.add(Calendar.DAY_OF_YEAR, 6)

            val formatter = SimpleDateFormat("dd MMM yyyy", localeId)
            "${formatter.format(start.time)} - ${formatter.format(end.time)}"
        }
    }

    private fun buildInsightText(
        context: Context,
        period: ReportPeriod,
        dayLabel: String,
        totalFullEvents: Int,
        affectedBinsCount: Int,
        topLocation: String,
        topTime: String
    ): String {
        if (totalFullEvents == 0 || affectedBinsCount == 0 || topLocation == "-") {
            return context.getString(R.string.report_priority_empty)
        }

        return if (period == ReportPeriod.DAILY) {
            context.getString(
                R.string.report_insight_daily,
                topLocation,
                dayLabel,
                topTime,
                totalFullEvents
            )
        } else {
            context.getString(R.string.report_insight_weekly, topLocation, topTime)
        }
    }

    private fun buildNoteText(context: Context, period: ReportPeriod): String {
        val firstLine = if (period == ReportPeriod.DAILY) {
            context.getString(R.string.report_note_daily)
        } else {
            context.getString(R.string.report_note_weekly)
        }
        return listOf(
            firstLine,
            context.getString(R.string.report_note_simulation)
        ).joinToString("\n")
    }
}
