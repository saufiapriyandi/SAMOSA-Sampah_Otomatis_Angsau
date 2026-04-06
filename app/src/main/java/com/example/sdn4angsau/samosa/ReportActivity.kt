package com.example.sdn4angsau.samosa

import android.content.ClipData
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowInsetsControllerCompat
import com.example.sdn4angsau.samosa.databinding.ActivityReportBinding

class ReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportBinding
    private var currentPeriod: ReportPeriod = ReportPeriod.DAILY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        binding.btnBackReport.setOnClickListener { finish() }
        binding.btnReportDaily.setOnClickListener {
            currentPeriod = ReportPeriod.DAILY
            renderReport()
        }
        binding.btnReportWeekly.setOnClickListener {
            currentPeriod = ReportPeriod.WEEKLY
            renderReport()
        }
        binding.btnShareReport.setOnClickListener {
            shareReport()
        }

        renderReport()
    }

    override fun onResume() {
        super.onResume()
        renderReport()
    }

    private fun renderReport() {
        val snapshot = LaporanSampahHelper.buildSnapshot(this, currentPeriod)

        binding.tvReportInfo.text = if (currentPeriod == ReportPeriod.DAILY) {
            getString(R.string.report_daily_info)
        } else {
            getString(R.string.report_weekly_info)
        }

        binding.tvReportPeriod.text = snapshot.periodLabel
        binding.tvReportGeneratedAt.text =
            getString(R.string.report_generated_at, snapshot.generatedAtLabel)
        binding.tvReportCountActive.text = snapshot.totalFullEvents.toString()
        binding.tvReportCountSafe.text = snapshot.affectedBinsCount.toString()
        binding.tvReportCountWarning.text = snapshot.topLocationLabel
        binding.tvReportCountFull.text = snapshot.topTimeLabel
        binding.tvReportInactiveInfo.text = snapshot.monitoredInfoLabel
        binding.tvReportPriority.text = snapshot.insightText
        binding.tvReportNote.text = snapshot.noteText

        renderTableRows(snapshot.tableRows)
        updatePeriodButtons()
    }

    private fun shareReport() {
        val reportText = LaporanSampahHelper.buildReport(this, currentPeriod)
        val reportFile = LaporanSampahHelper.exportReportFile(this, currentPeriod, reportText)
        val reportUri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            reportFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            clipData = ClipData.newRawUri(getString(R.string.report_title), reportUri)
            putExtra(Intent.EXTRA_STREAM, reportUri)
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.report_title))
            putExtra(Intent.EXTRA_TEXT, reportText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, getString(R.string.report_share_chooser)))
    }

    private fun updatePeriodButtons() {
        val selectedBackground = ContextCompat.getColor(this, R.color.green_primary)
        val selectedText = ContextCompat.getColor(this, R.color.white)
        val defaultBackground = ContextCompat.getColor(this, R.color.green_surface)
        val defaultText = ContextCompat.getColor(this, R.color.green_dark)
        val isDaily = currentPeriod == ReportPeriod.DAILY

        binding.btnReportDaily.backgroundTintList =
            ColorStateList.valueOf(if (isDaily) selectedBackground else defaultBackground)
        binding.btnReportWeekly.backgroundTintList =
            ColorStateList.valueOf(if (isDaily) defaultBackground else selectedBackground)
        binding.btnReportDaily.setTextColor(if (isDaily) selectedText else defaultText)
        binding.btnReportWeekly.setTextColor(if (isDaily) defaultText else selectedText)
    }

    private fun renderTableRows(rows: List<ReportTableRow>) {
        binding.layoutReportTableRows.removeAllViews()

        if (rows.isEmpty()) {
            binding.layoutReportTableRows.addView(
                TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    background = ContextCompat.getDrawable(
                        this@ReportActivity,
                        R.drawable.bg_report_table_row
                    )
                    text = getString(R.string.report_table_empty)
                    setTextColor(ContextCompat.getColor(this@ReportActivity, R.color.text_gray))
                    textSize = 13f
                }
            )
            return
        }

        rows.forEachIndexed { index, rowData ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = ContextCompat.getDrawable(
                    this@ReportActivity,
                    R.drawable.bg_report_table_row
                )
                setPadding(dp(12), dp(12), dp(12), dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (index > 0) topMargin = dp(8)
                }
            }

            val countColor = ContextCompat.getColor(this, R.color.red_warning)
            val timeColor = ContextCompat.getColor(this, R.color.green_dark)

            row.addView(
                createTableCell(
                    text = rowData.lokasi,
                    weight = 1.6f,
                    gravity = Gravity.START,
                    color = ContextCompat.getColor(this, R.color.text_dark),
                    bold = true
                )
            )
            row.addView(
                createTableCell(
                    text = rowData.binId,
                    weight = 0.9f,
                    gravity = Gravity.CENTER,
                    color = ContextCompat.getColor(this, R.color.text_gray)
                )
            )
            row.addView(
                createTableCell(
                    text = "${rowData.fullEventCount}x",
                    weight = 0.9f,
                    gravity = Gravity.CENTER,
                    color = countColor,
                    bold = true
                )
            )
            row.addView(
                createTableCell(
                    text = rowData.peakTimesLabel,
                    weight = 1.3f,
                    gravity = Gravity.END,
                    color = timeColor,
                    bold = true
                )
            )

            binding.layoutReportTableRows.addView(row)
        }
    }

    private fun createTableCell(
        text: String,
        weight: Float,
        gravity: Int,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)
            this.text = text
            this.gravity = gravity
            setTextColor(color)
            textSize = 13f
            maxLines = 2
            if (bold) {
                setTypeface(typeface, Typeface.BOLD)
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
