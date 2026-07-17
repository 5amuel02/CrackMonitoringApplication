// ═══════════════════════════════════════════════════════════════
//  AllDetectionsActivity.kt  (v2 — pakai DetectionSessionStore)
// ═══════════════════════════════════════════════════════════════
package com.example.seismicaplication

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class AllDetectionsActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DetectionLogAdapter
    private lateinit var progressBar: View
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var tvEmptySubtitle: TextView
    private lateinit var btnRetry: Button
    private lateinit var tvDataCount: TextView
    private lateinit var tvSessionInfo: TextView
    private lateinit var btnExportCsv: Button
    private lateinit var btnExportPdf: Button
    private lateinit var btnClearSession: Button

    private val BASE_URL get() = ServerConfig.getDataBaseUrl(this)

    // ✅ Tidak ada lagi sessionData di sini — semua pakai DetectionSessionStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_detections)
        initViews()
        setupBottomNavigation()
        fetchAndAccumulate()
        applyCurrentTheme()
    }

    override fun onResume() {
        super.onResume()
        // ✅ Setiap kali layar kembali ke foreground, tampilkan data terbaru dari store
        renderFromStore()
    }

    private fun initViews() {
        recyclerView     = findViewById(R.id.rv_all_detections)
        recyclerView.layoutManager = LinearLayoutManager(this)
        progressBar      = findViewById(R.id.progress_bar_all)
        layoutEmptyState = findViewById(R.id.layout_empty_state_all)
        tvEmptySubtitle  = findViewById(R.id.tv_empty_subtitle_all)
        btnRetry         = findViewById(R.id.btn_retry_all)
        tvDataCount      = findViewById(R.id.tv_data_count)
        tvSessionInfo    = findViewById(R.id.tv_session_info)
        btnExportCsv     = findViewById(R.id.btn_export_csv)
        btnExportPdf     = findViewById(R.id.btn_export_pdf)
        btnClearSession  = findViewById(R.id.btn_clear_session)

        btnRetry.setOnClickListener { fetchAndAccumulate() }
        btnExportCsv.setOnClickListener { exportToCsv() }
        btnExportPdf.setOnClickListener { exportToPdf() }

        btnClearSession.setOnClickListener {
            DetectionSessionStore.clear()           // ✅ bersihkan store global
            updateCountBadge()
            showEmptyState("Sesi dibersihkan.\nTarik ke bawah untuk memuat data baru.")
        }

        swipeRefresh = findViewById(R.id.swipe_refresh_all)
        swipeRefresh.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light
        )
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_card_blue)
        swipeRefresh.setOnRefreshListener { fetchAndAccumulate() }
    }

    // ── Network ────────────────────────────────────────────────

    private fun fetchAndAccumulate() {
        if (!swipeRefresh.isRefreshing) progressBar.visibility = View.VISIBLE

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
                    progressBar.visibility    = View.GONE
                    swipeRefresh.isRefreshing = false

                    if (response.isSuccessful && response.body() != null) {
                        val added = DetectionSessionStore.accumulate(response.body()!!)  // ✅ store global
                        Log.d("ALL_DETECTIONS", "Refresh #${DetectionSessionStore.refreshCount} — +$added baru, total: ${DetectionSessionStore.size()}")
                        renderFromStore()
                    } else {
                        if (DetectionSessionStore.isEmpty())
                            showEmptyState("Gagal mengambil data (HTTP ${response.code()}).\nTarik ke bawah untuk mencoba lagi.")
                        else {
                            Toast.makeText(this@AllDetectionsActivity,
                                "Gagal memuat data baru. Data sesi sebelumnya tetap tersimpan.",
                                Toast.LENGTH_SHORT).show()
                            renderFromStore()
                        }
                    }
                }

                override fun onFailure(call: Call<List<DetectionLog>>, t: Throwable) {
                    progressBar.visibility    = View.GONE
                    swipeRefresh.isRefreshing = false
                    if (DetectionSessionStore.isEmpty())
                        showEmptyState("Error koneksi: ${t.message}\nTarik ke bawah untuk mencoba lagi.")
                    else {
                        Toast.makeText(this@AllDetectionsActivity,
                            "Tidak dapat terhubung. Data sesi sebelumnya masih tersimpan.",
                            Toast.LENGTH_LONG).show()
                        renderFromStore()
                    }
                }
            })
    }

    // ── Render dari store ──────────────────────────────────────

    private fun renderFromStore() {
        updateCountBadge()
        val sorted = DetectionSessionStore.sortedByNewest()
        if (sorted.isEmpty()) {
            showEmptyState("Belum ada data deteksi.\nTarik ke bawah untuk memuat ulang.")
        } else {
            showList()
            adapter = DetectionLogAdapter(sorted)
            recyclerView.adapter = adapter
        }
    }

    // ── Export CSV ─────────────────────────────────────────────

    private fun exportToCsv() {
        if (DetectionSessionStore.isEmpty()) {
            Toast.makeText(this, "Tidak ada data untuk diekspor.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val sdf      = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "deteksi_seismik_${sdf.format(Date())}.csv"
            val csvFile  = File(cacheDir, fileName)

            FileWriter(csvFile).use { writer ->
                writer.appendLine("No,Timestamp,Label,Confidence,Status,Image_URL")
                DetectionSessionStore.sortedByNewest()
                    .forEachIndexed { index, log ->
                        val d = log.data
                        writer.appendLine(
                            "${index + 1}," +
                                    "${d.timestamp}," +
                                    "${d.label.replace(",", ";")}," +
                                    "${"%.4f".format(d.confidence)}," +
                                    "${(d.status ?: "-").replace(",", ";")}," +
                                    "${d.image_url ?: "-"}"
                        )
                    }
            }

            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", csvFile)
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Data Deteksi Seismik — $fileName")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Ekspor / Cetak Data Deteksi"
            ))
        } catch (e: Exception) {
            Log.e("ALL_DETECTIONS", "Gagal ekspor: ${e.message}")
            Toast.makeText(this, "Gagal mengekspor: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ── Export PDF ─────────────────────────────────────────────

    /** Warna severity berdasarkan confidence — selaras dengan ConfidenceGaugeView & DetectionLogAdapter. */
    private fun pdfSeverityColor(confidence: Double): Int = when {
        confidence >= 0.80 -> Color.parseColor("#16A34A")
        confidence >= 0.50 -> Color.parseColor("#D97706")
        else                -> Color.parseColor("#DC2626")
    }

    private fun exportToPdf() {
        if (DetectionSessionStore.isEmpty()) {
            Toast.makeText(this, "Tidak ada data untuk diekspor.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val pageWidth  = 595   // A4 @ 72dpi
            val pageHeight = 842
            val margin     = 36f
            val colNo      = margin
            val colTime    = margin + 28f
            val colLabel   = margin + 150f
            val colConf    = margin + 300f
            val colStatus  = margin + 390f

            val titlePaint   = Paint().apply { color = Color.parseColor("#0F172A"); textSize = 16f; isFakeBoldText = true }
            val subPaint     = Paint().apply { color = Color.parseColor("#64748B"); textSize = 10f }
            val headerBgPaint = Paint().apply { color = Color.parseColor("#142869") }
            val headerTxtPaint = Paint().apply { color = Color.WHITE; textSize = 10f; isFakeBoldText = true }
            val rowPaint     = Paint().apply { color = Color.parseColor("#0F172A"); textSize = 9f }
            val rowLinePaint = Paint().apply { color = Color.parseColor("#E2E8F0"); strokeWidth = 0.5f }
            val pagePaint    = Paint().apply { color = Color.parseColor("#94A3B8"); textSize = 8f }

            val sorted      = DetectionSessionStore.sortedByNewest()
            val confidences = sorted.map { it.data.confidence }
            val sdfFull     = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale("in", "ID"))

            val document = PdfDocument()
            var pageNumber = 1
            var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = page.canvas
            var y = margin

            fun drawTableHeader() {
                canvas.drawRect(margin, y - 12f, pageWidth - margin, y + 6f, headerBgPaint)
                canvas.drawText("No", colNo, y, headerTxtPaint)
                canvas.drawText("Waktu", colTime, y, headerTxtPaint)
                canvas.drawText("Label", colLabel, y, headerTxtPaint)
                canvas.drawText("Confidence", colConf, y, headerTxtPaint)
                canvas.drawText("Status", colStatus, y, headerTxtPaint)
                y += 22f
            }

            fun finishCurrentPage() {
                canvas.drawText("Halaman $pageNumber", pageWidth - margin - 50f, pageHeight - 20f, pagePaint)
                document.finishPage(page)
            }

            fun startNewPage() {
                finishCurrentPage()
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = margin
                drawTableHeader()
            }

            // ── Header laporan ──────────────────────────────────
            canvas.drawText("LAPORAN DETEKSI RETAK", margin, y, titlePaint)
            y += 18f
            canvas.drawText("AI Crack Detection System", margin, y, subPaint)
            y += 18f
            canvas.drawText("Dibuat: ${sdfFull.format(Date())}", margin, y, subPaint)
            y += 14f
            val avgConf = if (confidences.isNotEmpty()) confidences.average() * 100 else 0.0
            canvas.drawText(
                "Total deteksi: ${sorted.size}   ·   Rata-rata confidence: ${"%.1f".format(avgConf)}%",
                margin, y, subPaint
            )
            y += 20f
            drawTableHeader()

            // ── Baris data ───────────────────────────────────────
            sorted.forEachIndexed { index, log ->
                if (y > pageHeight - margin - 30f) startNewPage()

                val d = log.data
                rowPaint.color = Color.parseColor("#0F172A")
                canvas.drawText("${index + 1}", colNo, y, rowPaint)
                canvas.drawText(d.timestamp.replace("T", " ").take(16), colTime, y, rowPaint)
                canvas.drawText(d.label.uppercase().take(20), colLabel, y, rowPaint)

                rowPaint.color = pdfSeverityColor(d.confidence)
                canvas.drawText("${"%.1f".format(d.confidence * 100)}%", colConf, y, rowPaint)

                rowPaint.color = Color.parseColor("#0F172A")
                canvas.drawText((d.status ?: "-").take(14), colStatus, y, rowPaint)

                y += 16f
                canvas.drawLine(margin, y - 10f, pageWidth - margin, y - 10f, rowLinePaint)
            }
            finishCurrentPage()

            val sdf      = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "laporan_deteksi_${sdf.format(Date())}.pdf"
            val pdfFile  = File(cacheDir, fileName)
            FileOutputStream(pdfFile).use { document.writeTo(it) }
            document.close()

            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", pdfFile)
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Laporan Deteksi Retak — $fileName")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Ekspor / Cetak Laporan PDF"
            ))
        } catch (e: Exception) {
            Log.e("ALL_DETECTIONS", "Gagal ekspor PDF: ${e.message}")
            Toast.makeText(this, "Gagal mengekspor PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ── UI helpers ─────────────────────────────────────────────

    private fun updateCountBadge() {
        tvDataCount.text = "${DetectionSessionStore.size()} data tersimpan"
        val elapsed = (System.currentTimeMillis() - DetectionSessionStore.sessionStartTime) / 1000
        tvSessionInfo.text = "Sesi: ${elapsed / 60}m ${elapsed % 60}s · Refresh: ${DetectionSessionStore.refreshCount}×"
    }

    private fun showEmptyState(message: String) {
        recyclerView.visibility     = View.GONE
        layoutEmptyState.visibility = View.VISIBLE
        tvEmptySubtitle.text        = message
        updateCountBadge()
    }

    private fun showList() {
        recyclerView.visibility     = View.VISIBLE
        layoutEmptyState.visibility = View.GONE
        updateCountBadge()
    }

    // ── Bottom Nav ─────────────────────────────────────────────

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_logs
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_logs -> {
                    startActivity(Intent(this, DetectionLogActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }); overridePendingTransition(0, 0); finish(); true
                }
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