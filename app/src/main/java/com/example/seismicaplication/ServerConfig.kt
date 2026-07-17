package com.example.seismicaplication

import android.content.Context

object ServerConfig {

    /** Base URL untuk stream video → dipakai MainActivity (/video) */
    fun getStreamBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val ip   = prefs.getString(SettingsActivity.KEY_STREAM_IP, SettingsActivity.DEFAULT_STREAM_IP)
        val port = prefs.getString(SettingsActivity.KEY_STREAM_PORT, SettingsActivity.DEFAULT_STREAM_PORT)
        return "http://$ip:$port"
    }

    /** Full URL stream video → langsung pakai di WebView */
    fun getStreamVideoUrl(context: Context): String = "${getStreamBaseUrl(context)}/video"

    /** Base URL untuk data JSON → dipakai DetectionLog & Notification (/documents) */
    fun getDataBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val ip   = prefs.getString(SettingsActivity.KEY_DATA_IP, SettingsActivity.DEFAULT_DATA_IP)
        val port = prefs.getString(SettingsActivity.KEY_DATA_PORT, SettingsActivity.DEFAULT_DATA_PORT)
        return "http://$ip:$port/"
    }

    /** Interval polling dalam milidetik */
    fun getIntervalMs(context: Context): Long {
        val prefs = context.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val seconds = prefs.getString(SettingsActivity.KEY_INTERVAL, SettingsActivity.DEFAULT_INTERVAL)
            ?.toLongOrNull() ?: 5L
        return seconds * 1000L
    }
}