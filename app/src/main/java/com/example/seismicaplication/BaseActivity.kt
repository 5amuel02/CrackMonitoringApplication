package com.example.seismicaplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        applyCurrentTheme()
        startMascotIfReady()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MascotPermissionHelper.REQUEST_CODE) {
            if (MascotPermissionHelper.hasPermission(this)) {
                MascotService.start(this)
            }
        }
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    protected fun applyCurrentTheme() {
        val root: View = findViewById(android.R.id.content) ?: return
        ThemeManager.applyTheme(this, root)
    }

    // ── Mascot ────────────────────────────────────────────────────────────────

    private fun startMascotIfReady() {
        if (MascotPermissionHelper.hasPermission(this)) {
            MascotService.start(this)
        } else {
            MascotPermissionHelper.requestIfNeeded(this)
        }
    }
}