package com.example.sdn4angsau.samosa

import java.util.Calendar
import java.util.Locale

data class RiwayatPenuhEvent(
    val dayIndex: Int,
    val dayLabel: String,
    val timeLabel: String,
    val persentase: Int
)

object TempatSampahHistoryHelper {

    private val dayLabels = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat")
    private val timeLabels = listOf("08.00", "10.00", "12.00", "14.00", "16.00")

    fun getDayLabel(dayIndex: Int): String {
        return dayLabels[dayIndex.coerceIn(dayLabels.indices)]
    }

    fun getCurrentSchoolDayIndex(calendar: Calendar = Calendar.getInstance()): Int {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            else -> 4
        }
    }

    fun getDailyPercentages(
        bin: TempatSampah,
        dayIndex: Int,
        includeCurrentReading: Boolean = false
    ): List<Int> {
        val weekly = getWeeklyPercentages(bin, includeCurrentReading)
        return weekly[dayIndex.coerceIn(weekly.indices)]
    }

    fun getWeeklyPercentages(
        bin: TempatSampah,
        includeCurrentReading: Boolean = false
    ): List<List<Int>> {
        val currentDayIndex = getCurrentSchoolDayIndex()
        val source = when {
            normalize(bin.lokasi).contains("laboratorium") -> laboratoriumProfile()
            normalize(bin.lokasi).contains("kantor") -> ruangKantorProfile()
            normalize(bin.lokasi).contains("kantin") -> kantinProfile()
            normalize(bin.lokasi).contains("perpustakaan") -> perpustakaanProfile()
            normalize(bin.lokasi).contains("guru") -> ruangGuruProfile()
            else -> fallbackProfile(bin.persentase)
        }.map { it.toMutableList() }

        if (includeCurrentReading) {
            source[currentDayIndex][source[currentDayIndex].lastIndex] = bin.persentase.coerceIn(0, 100)
        }

        return source.map { values -> values.map { it.coerceIn(0, 100) } }
    }

    fun getFullEvents(bin: TempatSampah, period: ReportPeriod): List<RiwayatPenuhEvent> {
        val currentDayIndex = getCurrentSchoolDayIndex()
        val weeklyData = getWeeklyPercentages(bin, includeCurrentReading = false)
        val targetDays = if (period == ReportPeriod.DAILY) {
            listOf(currentDayIndex)
        } else {
            weeklyData.indices.toList()
        }

        val events = mutableListOf<RiwayatPenuhEvent>()
        targetDays.forEach { dayIndex ->
            weeklyData[dayIndex].forEachIndexed { timeIndex, persentase ->
                if (persentase >= 90) {
                    events += RiwayatPenuhEvent(
                        dayIndex = dayIndex,
                        dayLabel = getDayLabel(dayIndex),
                        timeLabel = timeLabels[timeIndex],
                        persentase = persentase
                    )
                }
            }
        }
        return events
    }

    fun buildPeakTimesLabel(events: List<RiwayatPenuhEvent>, maxItems: Int = 2): String {
        if (events.isEmpty()) return "-"

        val grouped = events.groupingBy { it.timeLabel }.eachCount()
        return grouped.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenBy { timeLabels.indexOf(it.key) }
            )
            .take(maxItems)
            .joinToString(", ") { it.key }
    }

    fun buildTopTimeLabel(events: List<RiwayatPenuhEvent>): String {
        if (events.isEmpty()) return "-"

        return events.groupingBy { it.timeLabel }
            .eachCount()
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenBy { timeLabels.indexOf(it.key) }
            )
            .first()
            .key
    }

    private fun normalize(value: String): String {
        return value.lowercase(Locale.getDefault())
    }

    private fun laboratoriumProfile(): List<List<Int>> {
        return listOf(
            listOf(42, 68, 86, 95, 100),
            listOf(50, 75, 92, 100, 100),
            listOf(38, 60, 80, 88, 94),
            listOf(45, 72, 90, 98, 100),
            listOf(30, 58, 82, 92, 97)
        )
    }

    private fun ruangKantorProfile(): List<List<Int>> {
        return listOf(
            listOf(28, 46, 70, 88, 95),
            listOf(20, 42, 60, 74, 82),
            listOf(35, 55, 78, 90, 96),
            listOf(30, 50, 76, 91, 95),
            listOf(18, 35, 58, 72, 80)
        )
    }

    private fun kantinProfile(): List<List<Int>> {
        return listOf(
            listOf(22, 48, 68, 76, 82),
            listOf(30, 58, 82, 90, 93),
            listOf(18, 40, 60, 70, 78),
            listOf(24, 45, 65, 75, 81),
            listOf(35, 62, 88, 96, 100)
        )
    }

    private fun perpustakaanProfile(): List<List<Int>> {
        return listOf(
            listOf(10, 20, 35, 42, 48),
            listOf(12, 22, 38, 45, 50),
            listOf(15, 25, 40, 48, 54),
            listOf(18, 28, 45, 52, 58),
            listOf(12, 24, 36, 44, 49)
        )
    }

    private fun ruangGuruProfile(): List<List<Int>> {
        return listOf(
            listOf(5, 10, 14, 18, 22),
            listOf(6, 12, 18, 20, 24),
            listOf(4, 9, 15, 19, 21),
            listOf(7, 13, 17, 21, 25),
            listOf(5, 11, 16, 20, 23)
        )
    }

    private fun fallbackProfile(currentPersentase: Int): List<List<Int>> {
        val base = currentPersentase.coerceIn(5, 100)
        val monday = listOf(base * 35 / 100, base * 55 / 100, base * 72 / 100, base * 86 / 100, base)
        val tuesday = listOf(base * 30 / 100, base * 50 / 100, base * 68 / 100, base * 82 / 100, (base + 4).coerceAtMost(100))
        val wednesday = listOf(base * 28 / 100, base * 48 / 100, base * 65 / 100, base * 78 / 100, (base - 2).coerceAtLeast(0))
        val thursday = listOf(base * 34 / 100, base * 54 / 100, base * 70 / 100, base * 84 / 100, (base + 2).coerceAtMost(100))
        val friday = listOf(base * 32 / 100, base * 52 / 100, base * 74 / 100, base * 88 / 100, (base + 6).coerceAtMost(100))
        return listOf(monday, tuesday, wednesday, thursday, friday)
    }
}
