package com.example.seismicaplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

// ✅ PERUBAHAN: extend BaseActivity (bukan AppCompatActivity)
class MainActivity : BaseActivity() {

    private val FLASK_STREAM_URL get() = ServerConfig.getStreamVideoUrl(this)

    private lateinit var webViewFeed: WebView
    private lateinit var tvOverlay: TextView
    private lateinit var tvDroneStatus: TextView
    private lateinit var tvYoloStatus: TextView
    private lateinit var iconDrone: ImageView
    private lateinit var iconYolo: ImageView
    private lateinit var btnLogs: Button
    private lateinit var btnRefresh: Button
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var streamStartTime: Long = 0
    private val latencyResults = mutableListOf<Long>()

    private val retryHandler = Handler(Looper.getMainLooper())
    private var retryRunnable: Runnable? = null
    private val RETRY_TIMEOUT_MS = 30_000L

    private var isPageError = false
    private var isStreamConnected = false

    // ─── Stream Latency Measurement ───────────────────────────────────────────
    private val latencyScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var latencyJob: Job? = null
    private val streamLatencies = mutableListOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupSwipeRefresh()
        setupVideoStream()
        setupButtons()

        // ✅ Apply theme setelah layout siap
        applyCurrentTheme()
    }

    private fun initViews() {
        webViewFeed        = findViewById(R.id.webview_feed)
        tvOverlay          = findViewById(R.id.tv_feed_overlay)
        tvDroneStatus      = findViewById(R.id.tv_drone_status)
        tvYoloStatus       = findViewById(R.id.tv_yolo_status)
        iconDrone          = findViewById(R.id.icon_drone_status)
        iconYolo           = findViewById(R.id.icon_yolo_status)
        btnLogs            = findViewById(R.id.btn_logs)
        btnRefresh         = findViewById(R.id.btn_refresh)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
    }

    // ─── Pull-to-Refresh ──────────────────────────────────────────────────────

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_light,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light
        )
        swipeRefreshLayout.setOnRefreshListener {
            Log.d("STREAM_REFRESH", "Pull-to-refresh dipicu.")
            refreshStream()
            Handler(Looper.getMainLooper()).postDelayed({
                swipeRefreshLayout.isRefreshing = false
            }, 1500)
        }
    }

    // ─── Stream Setup ─────────────────────────────────────────────────────────

    private fun setupVideoStream() {
        isStreamConnected = false
        isPageError       = false
        updateDroneStatus(false)
        updateYoloStatus(false)

        tvOverlay.text       = "Connecting..."
        tvOverlay.visibility = View.VISIBLE

        webViewFeed.settings.apply {
            javaScriptEnabled    = true
            loadWithOverviewMode = true
            useWideViewPort      = true
            setSupportZoom(false)
        }

        webViewFeed.webViewClient = object : WebViewClient() {

            override fun onPageStarted(
                view: WebView?,
                url: String?,
                favicon: android.graphics.Bitmap?
            ) {
                super.onPageStarted(view, url, favicon)
                isPageError = false
                Log.d("STREAM", "onPageStarted: $url")
            }

            @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.M)
            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    val code = error?.errorCode ?: -1
                    val desc = error?.description ?: "Unknown error"
                    Log.e("STREAM_ERROR", "onReceivedError (new) code=$code: $desc")
                    markStreamError()
                }
            }

            @Suppress("DEPRECATION")
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
                    Log.e("STREAM_ERROR", "onReceivedError (old) code=$errorCode: $description")
                    markStreamError()
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("STREAM", "onPageFinished: url=$url | isPageError=$isPageError")
                if (isPageError) {
                    markStreamError()
                    Log.w("STREAM", "onPageFinished: error dikonfirmasi, status merah.")
                }
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                Log.d("STREAM", "onPageCommitVisible: url=$url | isPageError=$isPageError")

                if (isPageError) {
                    Log.w("STREAM", "onPageCommitVisible diabaikan: isPageError=true")
                    return
                }

                if (url == null || !url.startsWith(FLASK_STREAM_URL.substringBefore("/", FLASK_STREAM_URL))) {
                    Log.w("STREAM", "onPageCommitVisible diabaikan: URL tidak cocok ($url)")
                    markStreamError()
                    return
                }

                retryRunnable?.let { retryHandler.removeCallbacks(it) }
                retryRunnable = null

                val latency = System.currentTimeMillis() - streamStartTime
                latencyResults.add(latency)
                Log.d("LATENCY_STREAM", "Percobaan ${latencyResults.size}: ${latency}ms")

                if (latencyResults.size % 5 == 0) {
                    Log.d("LATENCY_STREAM", "=== Ringkasan ${latencyResults.size}x ===")
                    Log.d("LATENCY_STREAM", "Rata-rata : %.1fms".format(latencyResults.average()))
                    Log.d("LATENCY_STREAM", "Min       : ${latencyResults.min()}ms")
                    Log.d("LATENCY_STREAM", "Max       : ${latencyResults.max()}ms")
                }

                tvOverlay.visibility = View.GONE
                isStreamConnected    = true
                updateDroneStatus(true)
                updateYoloStatus(true)
                startLatencyMeasurement() // ✅ Mulai ukur latency live setelah stream connect
            }
        }

        streamStartTime = System.currentTimeMillis()
        webViewFeed.loadUrl(FLASK_STREAM_URL)
        scheduleAutoRetry()
    }

    private fun markStreamError() {
        stopLatencyMeasurement() // ✅ Hentikan pengukuran saat error
        isPageError       = true
        isStreamConnected = false
        tvOverlay.text       = "Gagal terhubung. Tap Refresh atau tarik ke bawah untuk mencoba lagi."
        tvOverlay.visibility = View.VISIBLE
        updateDroneStatus(false)
        updateYoloStatus(false)
    }

    private fun refreshStream() {
        retryRunnable?.let { retryHandler.removeCallbacks(it) }
        retryRunnable = null
        stopLatencyMeasurement() // ✅ Stop dulu sebelum refresh

        Log.d("STREAM_REFRESH", "Memulai refresh stream...")

        isPageError       = false
        isStreamConnected = false
        updateDroneStatus(false)
        updateYoloStatus(false)

        tvOverlay.text       = "Reconnecting..."
        tvOverlay.visibility = View.VISIBLE

        streamStartTime = System.currentTimeMillis()
        webViewFeed.reload()
        scheduleAutoRetry()
    }

    private fun scheduleAutoRetry() {
        retryRunnable = Runnable {
            if (!isStreamConnected) {
                Log.d("STREAM_REFRESH", "Auto-retry karena timeout ${RETRY_TIMEOUT_MS}ms...")
                refreshStream()
            }
        }
        retryHandler.postDelayed(retryRunnable!!, RETRY_TIMEOUT_MS)
    }

    // ─── Live Latency Measurement ─────────────────────────────────────────────

    /**
     * Mulai periodic ping ke Flask server untuk ukur latency nyata.
     * Dipanggil setelah stream berhasil connect.
     */
    private fun startLatencyMeasurement() {
        latencyJob?.cancel()
        latencyJob = latencyScope.launch {
            while (isActive) {
                val latency = measurePingLatency(FLASK_STREAM_URL)
                if (latency >= 0) {
                    streamLatencies.add(latency)
                    Log.d("LATENCY_LIVE", "Ping #${streamLatencies.size}: ${latency}ms")

                    // Ringkasan setiap 5 ping
                    if (streamLatencies.size % 5 == 0) {
                        Log.d("LATENCY_LIVE", buildLatencySummary("Live (setiap 5 ping)"))
                    }
                } else {
                    Log.w("LATENCY_LIVE", "Ping gagal / timeout")
                }
                delay(3_000L) // ping tiap 3 detik
            }
        }
    }

    private fun stopLatencyMeasurement() {
        latencyJob?.cancel()
        latencyJob = null
    }

    /**
     * Ukur round-trip time ke Flask stream URL.
     * Pakai HEAD request supaya tidak download body video.
     */
    private fun measurePingLatency(urlString: String): Long {
        return try {
            val start = System.currentTimeMillis()
            val conn = URL(urlString).openConnection() as HttpURLConnection
            conn.requestMethod           = "HEAD"
            conn.connectTimeout          = 5_000
            conn.readTimeout             = 5_000
            conn.instanceFollowRedirects = false
            conn.connect()
            val code = conn.responseCode // tunggu response
            conn.disconnect()
            val elapsed = System.currentTimeMillis() - start

            // Flask stream biasanya 200 atau 206 (partial content)
            if (code in 200..299 || code == 206) elapsed else -1L
        } catch (e: Exception) {
            Log.e("LATENCY_LIVE", "Ping error: ${e.message}")
            -1L
        }
    }

    private fun buildLatencySummary(label: String): String {
        if (streamLatencies.isEmpty()) return "[$label] Belum ada data"
        return buildString {
            appendLine("===== LATENCY SUMMARY: $label =====")
            appendLine("Jumlah sample : ${streamLatencies.size}")
            appendLine("Rata-rata      : ${"%.1f".format(streamLatencies.average())}ms")
            appendLine("Min            : ${streamLatencies.min()}ms")
            appendLine("Max            : ${streamLatencies.max()}ms")
            val sorted = streamLatencies.sorted()
            val p95idx = (streamLatencies.size * 0.95).toInt().coerceAtMost(sorted.lastIndex)
            appendLine("P95            : ${sorted[p95idx]}ms")
            append("=".repeat(40))
        }
    }

    // ─── Buttons ──────────────────────────────────────────────────────────────

    private fun setupButtons() {
        btnLogs.setOnClickListener {
            startActivity(Intent(this, DetectionLogActivity::class.java))
        }

        btnRefresh.setOnClickListener {
            refreshStream()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home  -> true
                R.id.nav_logs  -> {
                    startActivity(Intent(this, DetectionLogActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_notif -> {
                    startActivity(Intent(this, NotificationActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    overridePendingTransition(0, 0); finish(); true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    overridePendingTransition(0, 0); finish(); true
                }
                else -> false
            }
        }
    }

    // ─── Latency Summary (Page Load) ──────────────────────────────────────────

    private fun printStreamLatencySummary() {
        if (latencyResults.isEmpty()) return
        Log.d("LATENCY_STREAM", "===== HASIL AKHIR PENGUJIAN LATENSI STREAM =====")
        latencyResults.forEachIndexed { index, value ->
            Log.d("LATENCY_STREAM", "Percobaan ${index + 1}: ${value}ms")
        }
        Log.d("LATENCY_STREAM", "Rata-rata : %.1fms".format(latencyResults.average()))
        Log.d("LATENCY_STREAM", "Min       : ${latencyResults.min()}ms")
        Log.d("LATENCY_STREAM", "Max       : ${latencyResults.max()}ms")
        Log.d("LATENCY_STREAM", "================================================")
    }

    // ─── Status Indicators ────────────────────────────────────────────────────

    private fun updateDroneStatus(isConnected: Boolean) {
        val green = ContextCompat.getColor(this, android.R.color.holo_green_light)
        val red   = ContextCompat.getColor(this, R.color.text_red)
        if (isConnected) {
            tvDroneStatus.text = "Connected"
            tvDroneStatus.setTextColor(green)
            iconDrone.setColorFilter(green)
        } else {
            tvDroneStatus.text = "Not Connected"
            tvDroneStatus.setTextColor(red)
            iconDrone.setColorFilter(red)
        }
    }

    private fun updateYoloStatus(isActive: Boolean) {
        val green = ContextCompat.getColor(this, android.R.color.holo_green_light)
        val red   = ContextCompat.getColor(this, R.color.text_red)
        if (isActive) {
            tvYoloStatus.text = "Active"
            tvYoloStatus.setTextColor(green)
            iconYolo.setColorFilter(green)
        } else {
            tvYoloStatus.text = "Not Active"
            tvYoloStatus.setTextColor(red)
            iconYolo.setColorFilter(red)
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onPause() {
        super.onPause()
        webViewFeed.onPause()
        printStreamLatencySummary()
        retryRunnable?.let { retryHandler.removeCallbacks(it) }
        stopLatencyMeasurement() // ✅ Stop saat activity pause
    }

    override fun onResume() {
        super.onResume()   // ← BaseActivity.onResume() sudah memanggil applyCurrentTheme()
        webViewFeed.onResume()

        if (!isStreamConnected) {
            Log.d("STREAM", "onResume: stream belum aktif, reload...")
            isPageError       = false
            isStreamConnected = false
            updateDroneStatus(false)
            updateYoloStatus(false)
            tvOverlay.text       = "Reconnecting..."
            tvOverlay.visibility = View.VISIBLE

            streamStartTime = System.currentTimeMillis()
            webViewFeed.reload()
            scheduleAutoRetry()
        } else {
            Log.d("STREAM", "onResume: stream masih aktif, tidak perlu reload.")
            startLatencyMeasurement() // ✅ Lanjut ukur latency setelah resume
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        retryRunnable?.let { retryHandler.removeCallbacks(it) }
        stopLatencyMeasurement()
        Log.d("LATENCY_LIVE", buildLatencySummary("Final")) // ✅ Ringkasan akhir saat destroy
        latencyScope.cancel()
        webViewFeed.destroy()
    }
}