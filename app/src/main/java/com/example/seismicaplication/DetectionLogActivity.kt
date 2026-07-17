// ═══════════════════════════════════════════════════════════════
//  DetectionLogActivity.kt  (v3 — real-time polling, max 10 data)
// ═══════════════════════════════════════════════════════════════
package com.example.seismicaplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Locale

class DetectionLogActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DetectionLogAdapter
    private lateinit var progressBar: View
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var tvEmptySubtitle: TextView
    private lateinit var btnRetry: Button
    private lateinit var btnAllDetections: Button
    private lateinit var tvLiveIndicator: TextView

    private val BASE_URL get() = ServerConfig.getDataBaseUrl(this)
    private val DEDUP_WINDOW_MS = 5_000L
    private val MAX_DISPLAY = 10                  // maksimal item ditampilkan
    private val POLL_INTERVAL_MS = 5_000L         // polling setiap 5 detik

    // ── Polling handler ────────────────────────────────────────
    private val handler = Handler(Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            fetchDataFromFlask(isPolling = true)
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    private val latencyResults = mutableListOf<Long>()
    private var fetchCount = 0
    private var isFirstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detection_log)
        initViews()
        setupBottomNavigation()
        fetchDataFromFlask(isPolling = false)   // load pertama dengan loading indicator
        applyCurrentTheme()
    }

    override fun onResume() {
        super.onResume()
        // ✅ Mulai polling saat layar aktif
        handler.postDelayed(pollRunnable, POLL_INTERVAL_MS)
        setLiveIndicator(true)
    }

    override fun onPause() {
        super.onPause()
        // ✅ Hentikan polling saat layar tidak aktif (hemat baterai)
        handler.removeCallbacks(pollRunnable)
        setLiveIndicator(false)
    }

    // ── Init ───────────────────────────────────────────────────

    private fun initViews() {
        recyclerView     = findViewById(R.id.rv_detection_logs)
        recyclerView.layoutManager = LinearLayoutManager(this)
        progressBar      = findViewById(R.id.progress_bar)
        layoutEmptyState = findViewById(R.id.layout_empty_state)
        tvEmptySubtitle  = findViewById(R.id.tv_empty_subtitle)
        btnRetry         = findViewById(R.id.btn_retry)
        btnAllDetections = findViewById(R.id.btn_all_detections)
        tvLiveIndicator  = findViewById(R.id.tv_live_indicator)

        btnRetry.setOnClickListener {
            fetchDataFromFlask(isPolling = false)
        }
        btnAllDetections.setOnClickListener {
            startActivity(Intent(this, AllDetectionsActivity::class.java))
        }

        swipeRefresh = findViewById(R.id.swipe_refresh)
        swipeRefresh.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light
        )
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_card_blue)
        swipeRefresh.setOnRefreshListener {
            fetchDataFromFlask(isPolling = false)
        }
    }

    // ── Network ────────────────────────────────────────────────

    private fun fetchDataFromFlask(isPolling: Boolean) {
        // Polling background: tidak tampilkan progressBar / swipe indicator
        if (!isPolling && !swipeRefresh.isRefreshing) {
            progressBar.visibility = View.VISIBLE
        }

        val startTime = System.currentTimeMillis()
        fetchCount++

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
            .getDetectionLogs()
            .enqueue(object : Callback<List<DetectionLog>> {

                override fun onResponse(
                    call: Call<List<DetectionLog>>,
                    response: Response<List<DetectionLog>>
                ) {
                    val latency = System.currentTimeMillis() - startTime
                    latencyResults.add(latency)
                    Log.d("LATENCY_LOG", "#$fetchCount: ${latency}ms — HTTP ${response.code()}")
                    if (latencyResults.size % 5 == 0) printLogLatencySummary()

                    progressBar.visibility    = View.GONE
                    swipeRefresh.isRefreshing = false

                    if (response.isSuccessful && response.body() != null) {
                        val rawList = response.body()!!

                        // ✅ Akumulasi ke store global (untuk halaman "Semua Data")
                        DetectionSessionStore.accumulate(rawList)

                        // ✅ Tampilkan hanya MAX_DISPLAY item terbaru di halaman ini
                        val display = deduplicateByTimeWindow(
                            rawList.filter { it.data.confidence >= 0.5 }
                        )
                            .sortedByDescending { parseTs(it.data.timestamp) }
                            .take(MAX_DISPLAY)

                        if (display.isEmpty()) {
                            if (isFirstLoad) showEmptyState("Tidak ada data deteksi.\nMenunggu data baru...")
                        } else {
                            isFirstLoad = false
                            showList()
                            // ✅ Animasi: hanya update adapter kalau data berubah
                            if (!isSameList(display)) {
                                adapter = DetectionLogAdapter(display)
                                recyclerView.adapter = adapter
                                // Scroll ke atas untuk tampilkan data terbaru
                                recyclerView.scrollToPosition(0)
                            }
                        }
                    } else {
                        if (isFirstLoad)
                            showEmptyState("Gagal mengambil data (HTTP ${response.code()}).")
                    }
                }

                override fun onFailure(call: Call<List<DetectionLog>>, t: Throwable) {
                    val latency = System.currentTimeMillis() - startTime
                    Log.d("LATENCY_LOG", "#$fetchCount GAGAL ${latency}ms — ${t.message}")
                    progressBar.visibility    = View.GONE
                    swipeRefresh.isRefreshing = false
                    if (isFirstLoad)
                        showEmptyState("Error koneksi: ${t.message}\nTarik ke bawah untuk mencoba lagi.")
                }
            })
    }

    // ── Helpers ────────────────────────────────────────────────

    /**
     * Cek apakah list yang akan ditampilkan sama dengan yang sudah ada di adapter
     * (hindari flicker update yang tidak perlu).
     */
    private fun isSameList(newList: List<DetectionLog>): Boolean {
        if (!::adapter.isInitialized) return false
        val current = (0 until adapter.itemCount).map {
            (recyclerView.adapter as? DetectionLogAdapter)
        }
        // Bandingkan berdasarkan timestamp item pertama saja (cukup untuk deteksi data baru)
        val currentFirst = try {
            (recyclerView.adapter as DetectionLogAdapter).getFirstTimestamp()
        } catch (e: Exception) { "" }
        val newFirst = newList.firstOrNull()?.data?.timestamp ?: ""
        return currentFirst == newFirst && adapter.itemCount == newList.size
    }

    private fun setLiveIndicator(isLive: Boolean) {
        if (::tvLiveIndicator.isInitialized) {
            tvLiveIndicator.text = if (isLive) "🔴 LIVE" else "⏸ PAUSED"
            tvLiveIndicator.setTextColor(
                if (isLive) 0xFF4CAF50.toInt() else 0xFF9E9E9E.toInt()
            )
        }
    }

    private fun parseTs(ts: String): Long = try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            .parse(ts.substringBefore("."))?.time ?: 0L
    } catch (e: Exception) { 0L }

    private fun deduplicateByTimeWindow(list: List<DetectionLog>): List<DetectionLog> {
        if (list.isEmpty()) return emptyList()
        val sorted = list.sortedBy { parseTs(it.data.timestamp) }
        val result = mutableListOf<DetectionLog>()
        val group  = mutableListOf<DetectionLog>()
        for (item in sorted) {
            if (group.isEmpty()) { group.add(item); continue }
            val last = group.last()
            val diff = parseTs(item.data.timestamp) - parseTs(last.data.timestamp)
            val same = item.data.label.equals(last.data.label, ignoreCase = true)
            if (same && diff <= DEDUP_WINDOW_MS) group.add(item)
            else {
                result.add(group.maxByOrNull { it.data.confidence }!!)
                group.clear(); group.add(item)
            }
        }
        if (group.isNotEmpty()) result.add(group.maxByOrNull { it.data.confidence }!!)
        return result
    }

    private fun showEmptyState(message: String) {
        recyclerView.visibility     = View.GONE
        layoutEmptyState.visibility = View.VISIBLE
        tvEmptySubtitle.text        = message
    }

    private fun showList() {
        recyclerView.visibility     = View.VISIBLE
        layoutEmptyState.visibility = View.GONE
    }

    private fun printLogLatencySummary() {
        if (latencyResults.isEmpty()) return
        Log.d("LATENCY_LOG", "===== RINGKASAN LATENSI (${latencyResults.size}x) =====")
        Log.d("LATENCY_LOG", "  Rata-rata : %.1fms".format(latencyResults.average()))
        Log.d("LATENCY_LOG", "  Min       : ${latencyResults.min()}ms")
        Log.d("LATENCY_LOG", "  Max       : ${latencyResults.max()}ms")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollRunnable)
        printLogLatencySummary()
    }

    // ── Bottom Nav ─────────────────────────────────────────────

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_logs
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_logs -> true
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }); overridePendingTransition(0, 0); finish(); true
                }
                R.id.nav_notif -> {
                    startActivity(Intent(this, NotificationActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }); overridePendingTransition(0, 0); finish(); true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }); overridePendingTransition(0, 0); finish(); true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }); overridePendingTransition(0, 0); finish(); true
                }
                else -> false
            }
        }
    }
}