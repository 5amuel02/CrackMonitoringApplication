package com.example.seismicaplication

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class NotificationActivity : BaseActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvScanStart: TextView
    private lateinit var lineChart: LineChart
    private lateinit var gaugeView: ConfidenceGaugeView
    private lateinit var tvGaugeSampleCount: TextView
    private lateinit var layoutLive: LinearLayout
    private lateinit var layoutSummary: LinearLayout
    private lateinit var btnShowSummary: MaterialButton
    private lateinit var btnBackToLive: MaterialButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var btnRefreshOffline: MaterialButton

    private lateinit var tvSummaryStart: TextView
    private lateinit var tvSummaryEnd: TextView
    private lateinit var tvSummaryTotal: TextView
    private lateinit var tvSummaryAvgConf: TextView
    private lateinit var tvSummaryTopLabel: TextView
    private lateinit var tvSummaryMinConf: TextView
    private lateinit var tvSummaryMaxConf: TextView
    // ✅ Timeline LineChart di summary (ganti barChartSummary)
    private lateinit var lineChartSummary: LineChart
    private lateinit var tvTimelineRange: TextView

    private val BASE_URL get() = ServerConfig.getDataBaseUrl(this)
    private val REFRESH_INTERVAL_MS get() = ServerConfig.getIntervalMs(this)
    private val MAX_FAILURES = 0.5
    private val MAX_LINE_POINTS = 20

    private val handler = Handler(Looper.getMainLooper())
    private var serverAlive = true
    private var consecutiveFailures = 0

    private var scanStartTime: String = ""
    private var scanEndTime: String = ""

    private val allDetections = mutableListOf<DetectionLog>()
    private val lineConfidenceEntries = mutableListOf<Entry>()
    private var lineIndex = 0f

    // ✅ Timeline: simpan pasangan (timestamp singkat, confidence) per polling
    private val timelineEntries = mutableListOf<Entry>()          // Entry untuk chart
    private val timelineLabels  = mutableListOf<String>()         // Label waktu sumbu X
    private var timelineIndex   = 0f

    private val allConfidenceValues = mutableListOf<Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)
        initViews()
        setupBottomNavigation()
        setupCharts()
        scanStartTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        tvScanStart.text = "Mulai: $scanStartTime"
        startPolling()
        applyCurrentTheme()
    }

    private fun initViews() {
        tvStatus           = findViewById(R.id.tv_status)
        tvScanStart        = findViewById(R.id.tv_scan_start)
        lineChart          = findViewById(R.id.line_chart)
        gaugeView          = findViewById(R.id.gauge_confidence)
        tvGaugeSampleCount = findViewById(R.id.tv_gauge_sample_count)
        layoutLive         = findViewById(R.id.layout_live)
        layoutSummary      = findViewById(R.id.layout_summary)
        btnShowSummary     = findViewById(R.id.btn_show_summary)
        btnBackToLive      = findViewById(R.id.btn_back_to_live)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        btnRefreshOffline  = findViewById(R.id.btn_refresh_offline)

        tvSummaryStart    = findViewById(R.id.tv_summary_start)
        tvSummaryEnd      = findViewById(R.id.tv_summary_end)
        tvSummaryTotal    = findViewById(R.id.tv_summary_total)
        tvSummaryAvgConf  = findViewById(R.id.tv_summary_avg_conf)
        tvSummaryTopLabel = findViewById(R.id.tv_summary_top_label)
        tvSummaryMinConf  = findViewById(R.id.tv_summary_min_conf)
        tvSummaryMaxConf  = findViewById(R.id.tv_summary_max_conf)
        // ✅ Timeline chart
        lineChartSummary  = findViewById(R.id.line_chart_summary)
        tvTimelineRange   = findViewById(R.id.tv_timeline_range)

        swipeRefreshLayout.setColorSchemeColors(
            Color.parseColor("#2196F3"), Color.parseColor("#4CAF50")
        )
        swipeRefreshLayout.setOnRefreshListener { tryReconnect() }
        btnRefreshOffline.setOnClickListener { tryReconnect() }
        btnShowSummary.setOnClickListener { showSummary(manualTrigger = true) }
        btnBackToLive.setOnClickListener {
            layoutSummary.visibility  = View.GONE
            layoutLive.visibility     = View.VISIBLE
            btnShowSummary.visibility = View.VISIBLE
        }
    }

    private fun tryReconnect() {
        tvStatus.text = "● Menghubungkan..."
        tvStatus.setTextColor(Color.parseColor("#FF9800"))
        Retrofit.Builder().baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(ApiService::class.java).getDetectionLogs()
            .enqueue(object : Callback<List<DetectionLog>> {
                override fun onResponse(call: Call<List<DetectionLog>>, response: Response<List<DetectionLog>>) {
                    swipeRefreshLayout.isRefreshing = false
                    if (response.isSuccessful && response.body() != null) {
                        consecutiveFailures = 0; serverAlive = true
                        tvStatus.text = "● LIVE"; tvStatus.setTextColor(Color.parseColor("#4CAF50"))
                        btnRefreshOffline.visibility = View.GONE
                        layoutSummary.visibility = View.GONE
                        layoutLive.visibility = View.VISIBLE
                        btnShowSummary.visibility = View.VISIBLE
                        scanStartTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        tvScanStart.text = "Mulai: $scanStartTime"
                        processNewData(response.body()!!)
                        startPolling()
                    } else { onReconnectFailed() }
                }
                override fun onFailure(call: Call<List<DetectionLog>>, t: Throwable) {
                    swipeRefreshLayout.isRefreshing = false; onReconnectFailed()
                }
            })
    }

    private fun onReconnectFailed() {
        tvStatus.text = "● OFFLINE"
        tvStatus.setTextColor(Color.parseColor("#F44336"))
        btnRefreshOffline.visibility = View.VISIBLE
    }

    private fun setupCharts() {
        val dark = ThemeManager.isDarkMode(this)
        val axisColor = if (dark) Color.GRAY else Color.parseColor("#455A64")

        // ── Live line chart ────────────────────────────────────
        lineChart.apply {
            description.isEnabled = false; legend.isEnabled = false; setTouchEnabled(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false); xAxis.textColor = axisColor
            axisLeft.textColor = axisColor; axisLeft.axisMinimum = 0f; axisLeft.axisMaximum = 1f
            axisRight.isEnabled = false; setExtraOffsets(8f, 8f, 8f, 8f)
        }

        // ── Summary timeline chart ─────────────────────────────
        lineChartSummary.apply {
            description.isEnabled = false; legend.isEnabled = false; setTouchEnabled(true)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(true)
            xAxis.gridColor = Color.parseColor("#1A3060")
            xAxis.textColor = axisColor
            xAxis.granularity = 1f
            xAxis.labelRotationAngle = -35f
            axisLeft.textColor = axisColor
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = 1f
            axisLeft.setDrawGridLines(true)
            axisLeft.gridColor = Color.parseColor("#1A3060")
            axisRight.isEnabled = false
            setExtraOffsets(8f, 12f, 8f, 16f)

            // Garis threshold 50%
            val limit = LimitLine(0.5f, "Min 50%").apply {
                lineColor = Color.parseColor("#FF9800")
                lineWidth = 1f
                textColor = Color.parseColor("#FF9800")
                textSize = 9f
                enableDashedLine(10f, 5f, 0f)
            }
            axisLeft.addLimitLine(limit)
        }
    }

    private val pollRunnable = object : Runnable {
        override fun run() { fetchData(); handler.postDelayed(this, REFRESH_INTERVAL_MS) }
    }

    private fun startPolling() { handler.post(pollRunnable) }
    private fun stopPolling()  { handler.removeCallbacks(pollRunnable) }

    private fun fetchData() {
        Retrofit.Builder().baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(ApiService::class.java).getDetectionLogs()
            .enqueue(object : Callback<List<DetectionLog>> {
                override fun onResponse(call: Call<List<DetectionLog>>, response: Response<List<DetectionLog>>) {
                    if (response.isSuccessful && response.body() != null) {
                        consecutiveFailures = 0; serverAlive = true
                        tvStatus.text = "● LIVE"; tvStatus.setTextColor(Color.parseColor("#4CAF50"))
                        btnRefreshOffline.visibility = View.GONE
                        processNewData(response.body()!!)
                    } else { handleFailure() }
                }
                override fun onFailure(call: Call<List<DetectionLog>>, t: Throwable) {
                    Log.w("NOTIF", "Fetch gagal: ${t.message}"); handleFailure()
                }
            })
    }

    private fun handleFailure() {
        consecutiveFailures++
        if (consecutiveFailures >= MAX_FAILURES && serverAlive) {
            serverAlive = false; stopPolling()
            tvStatus.text = "● OFFLINE"; tvStatus.setTextColor(Color.parseColor("#F44336"))
            btnRefreshOffline.visibility = View.VISIBLE
            showSummary(manualTrigger = false)
        }
    }

    private fun processNewData(items: List<DetectionLog>) {
        allDetections.addAll(items)
        val avgConf = items.map { it.data.confidence.toFloat() }.average().toFloat()

        // Live line chart
        lineConfidenceEntries.add(Entry(lineIndex++, avgConf))
        if (lineConfidenceEntries.size > MAX_LINE_POINTS) {
            lineConfidenceEntries.removeAt(0)
            lineConfidenceEntries.forEachIndexed { i, e ->
                lineConfidenceEntries[i] = Entry(i.toFloat(), e.y)
            }
            lineIndex = lineConfidenceEntries.size.toFloat()
        }

        // ✅ Timeline: rekam waktu + confidence rata-rata tiap polling
        val timeLabel = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        timelineEntries.add(Entry(timelineIndex++, avgConf))
        timelineLabels.add(timeLabel)

        // Gauge
        items.forEach { allConfidenceValues.add(it.data.confidence.toFloat()) }
        val overallAvg = if (allConfidenceValues.isNotEmpty())
            allConfidenceValues.average().toFloat() else 0f
        gaugeView.setConfidence(overallAvg)
        tvGaugeSampleCount.text = "dari ${allConfidenceValues.size} sampel deteksi"

        updateLineChart()
    }

    private fun updateLineChart() {
        val dataSet = LineDataSet(lineConfidenceEntries.toList(), "Confidence").apply {
            color = Color.parseColor("#2196F3"); setCircleColor(Color.parseColor("#2196F3"))
            lineWidth = 2f; circleRadius = 3f; setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        lineChart.data = LineData(dataSet); lineChart.invalidate()
    }

    private fun showSummary(manualTrigger: Boolean) {
        scanEndTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        layoutLive.visibility     = View.GONE
        layoutSummary.visibility  = View.VISIBLE
        btnShowSummary.visibility = View.GONE
        btnBackToLive.visibility  = if (manualTrigger && serverAlive) View.VISIBLE else View.GONE

        if (allDetections.isEmpty()) {
            tvSummaryStart.text    = "Mulai   : $scanStartTime"
            tvSummaryEnd.text      = "Selesai : $scanEndTime"
            tvSummaryTotal.text    = "Total deteksi : 0"
            tvSummaryAvgConf.text  = "Tidak ada data"
            tvSummaryTopLabel.text = "-"; tvSummaryMinConf.text = "-"; tvSummaryMaxConf.text = "-"
            return
        }

        val confidences = allDetections.map { it.data.confidence }

        // Waktu polling dengan confidence rata-rata tertinggi
        val peakEntry = timelineEntries.maxByOrNull { it.y }
        val peakTime  = peakEntry?.let {
            val idx = it.x.toInt()
            if (idx >= 0 && idx < timelineLabels.size) timelineLabels[idx] else "-"
        } ?: "-"
        val peakPct = peakEntry?.let { "${"%.1f".format(it.y * 100)}%" } ?: "-"

        tvSummaryStart.text    = "Mulai   : $scanStartTime"
        tvSummaryEnd.text      = "Selesai : $scanEndTime"
        tvSummaryTotal.text    = "Total deteksi        : ${allDetections.size}"
        tvSummaryAvgConf.text  = "Rata-rata confidence : ${"%.1f".format(confidences.average() * 100)}%"
        tvSummaryMinConf.text  = "Confidence terendah  : ${"%.1f".format(confidences.min() * 100)}%"
        tvSummaryMaxConf.text  = "Confidence tertinggi : ${"%.1f".format(confidences.max() * 100)}%"
        tvSummaryTopLabel.text = "Puncak deteksi       : $peakPct pukul $peakTime"

        // ✅ Render timeline chart
        updateTimelineChart()
    }

    // ✅ Timeline chart: confidence rata-rata per polling sepanjang sesi
    private fun updateTimelineChart() {
        if (timelineEntries.isEmpty()) return

        val dark = ThemeManager.isDarkMode(this)
        val axisColor = if (dark) Color.GRAY else Color.parseColor("#455A64")

        // Warna titik berdasarkan nilai: merah < 0.5, kuning < 0.7, hijau >= 0.7
        val pointColors = timelineEntries.map { entry ->
            when {
                entry.y >= 0.70f -> Color.parseColor("#4CAF50")
                entry.y >= 0.50f -> Color.parseColor("#FFEB3B")
                else             -> Color.parseColor("#F44336")
            }
        }

        val dataSet = LineDataSet(timelineEntries.toList(), "Confidence per Polling").apply {
            color = Color.parseColor("#42A5F5")
            lineWidth = 2f
            circleRadius = 5f
            setCircleColors(pointColors)
            circleHoleColor = Color.parseColor("#0D2560")
            circleHoleRadius = 2.5f
            setDrawValues(true)
            valueTextSize = 8f
            valueTextColor = if (dark) Color.WHITE else Color.parseColor("#0D1B40")
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) =
                    "${"%.0f".format(value * 100)}%"
            }
            mode = LineDataSet.Mode.CUBIC_BEZIER
            // Fill area di bawah garis
            setDrawFilled(true)
            fillColor = Color.parseColor("#42A5F5")
            fillAlpha = 40
        }

        // Label sumbu X = waktu polling
        lineChartSummary.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val idx = value.toInt()
                return if (idx >= 0 && idx < timelineLabels.size) timelineLabels[idx] else ""
            }
        }
        lineChartSummary.xAxis.textColor = axisColor

        // Tampilkan label hanya sebagian kalau data banyak (hindari crowded)
        val labelCount = minOf(timelineEntries.size, 6)
        lineChartSummary.xAxis.setLabelCount(labelCount, true)

        lineChartSummary.data = LineData(dataSet)
        lineChartSummary.animateX(600)
        lineChartSummary.invalidate()

        // Info range waktu di header card
        if (timelineLabels.isNotEmpty()) {
            tvTimelineRange.text = "${timelineLabels.first()} → ${timelineLabels.last()}"
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_notif
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_notif -> true
                R.id.nav_home -> { startActivity(Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP }); overridePendingTransition(0, 0); finish(); true }
                R.id.nav_logs -> { startActivity(Intent(this, DetectionLogActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP }); overridePendingTransition(0, 0); finish(); true }
                R.id.nav_profile -> { startActivity(Intent(this, ProfileActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP }); overridePendingTransition(0, 0); finish(); true }
                R.id.nav_settings -> { startActivity(Intent(this, SettingsActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP }); overridePendingTransition(0, 0); finish(); true }
                else -> false
            }
        }
    }

    override fun onPause()   { super.onPause();   stopPolling() }
    override fun onResume()  { super.onResume();  if (serverAlive) startPolling() }
    override fun onDestroy() { super.onDestroy(); stopPolling() }
}