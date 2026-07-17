// ═══════════════════════════════════════════════════════════════
//  DetectionSessionStore.kt
//  Singleton penyimpan data deteksi selama proses app hidup.
//  Data tidak akan hilang meski berpindah antar Activity.
// ═══════════════════════════════════════════════════════════════
package com.example.seismicaplication

import java.text.SimpleDateFormat
import java.util.Locale

object DetectionSessionStore {

    // ── Storage utama ──────────────────────────────────────────
    private val _sessionData = mutableListOf<DetectionLog>()
    val sessionData: List<DetectionLog> get() = _sessionData.toList()

    val sessionStartTime: Long = System.currentTimeMillis()
    var refreshCount: Int = 0
        private set

    private val DEDUP_WINDOW_MS = 5_000L

    // ── Tambahkan data baru (akumulasi, tanpa duplikat) ────────
    fun accumulate(incoming: List<DetectionLog>): Int {
        refreshCount++
        // ✅ Tidak ada filter confidence — semua data 0-100% masuk
        val deduped = deduplicateByTimeWindow(incoming)

        var added = 0
        for (item in deduped) {
            val exists = _sessionData.any {
                it.data.timestamp == item.data.timestamp &&
                        it.data.label.equals(item.data.label, ignoreCase = true)
            }
            if (!exists) { _sessionData.add(item); added++ }
        }
        return added
    }

    // ── Reset sesi ─────────────────────────────────────────────
    fun clear() {
        _sessionData.clear()
        refreshCount = 0
    }

    // ── Helpers ────────────────────────────────────────────────
    fun sortedByNewest(): List<DetectionLog> =
        _sessionData.sortedByDescending { parseTimestamp(it.data.timestamp) }

    fun size() = _sessionData.size

    fun isEmpty() = _sessionData.isEmpty()

    fun parseTimestamp(ts: String): Long {
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .parse(ts.substringBefore("."))?.time ?: 0L
        } catch (e: Exception) { 0L }
    }

    private fun deduplicateByTimeWindow(list: List<DetectionLog>): List<DetectionLog> {
        if (list.isEmpty()) return emptyList()
        val sorted = list.sortedBy { parseTimestamp(it.data.timestamp) }
        val result = mutableListOf<DetectionLog>()
        val group  = mutableListOf<DetectionLog>()
        for (item in sorted) {
            if (group.isEmpty()) { group.add(item); continue }
            val last = group.last()
            val diff = parseTimestamp(item.data.timestamp) - parseTimestamp(last.data.timestamp)
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
}