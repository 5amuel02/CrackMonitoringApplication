package com.example.seismicaplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * MascotPermissionHelper — mengurus permission SYSTEM_ALERT_WINDOW.
 *
 * Permission ini wajib untuk menampilkan floating overlay di atas semua app.
 * Di Android 6+, user harus grant manual dari Settings.
 *
 * Cara pakai di BaseActivity:
 *   MascotPermissionHelper.requestIfNeeded(this)
 */
object MascotPermissionHelper {

    fun hasPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true   // Android < 6 tidak perlu permission ini
        }
    }

    /**
     * Jika belum ada permission, buka Settings agar user bisa grant.
     * Return true jika sudah punya permission (langsung bisa start mascot).
     */
    fun requestIfNeeded(activity: Activity): Boolean {
        if (hasPermission(activity)) return true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivityForResult(intent, REQUEST_CODE)
        }
        return false
    }

    const val REQUEST_CODE = 2001
}