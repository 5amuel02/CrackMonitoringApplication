package com.example.seismicaplication

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * ThemeManager — mengelola Dark / Light mode secara manual.
 *
 * Dark  : bg biru tua (#0A1940), card biru (#0D2560), teks putih
 * Light : bg biru muda (#E8F0FE), card putih (#FFFFFF), teks hitam
 *
 * Cara pakai di setiap Activity:
 *   override fun onResume() {
 *       super.onResume()
 *       ThemeManager.applyTheme(this, findViewById(android.R.id.content))
 *   }
 */
object ThemeManager {

    // ── Dark palette (default / sekarang) — tema "AI scan": slate + cyan ─────
    private val DARK_BG              = Color.parseColor("#0A0E16")
    private val DARK_CARD            = Color.parseColor("#121A33")
    private val DARK_HEADER          = Color.parseColor("#142869")
    private val DARK_BTN             = Color.parseColor("#0EA5E9")
    private val DARK_TEXT_PRIMARY    = Color.parseColor("#F1F5F9")
    private val DARK_TEXT_SECONDARY  = Color.parseColor("#94A3B8")
    private val DARK_ICON_TINT       = Color.parseColor("#22D3EE")   // cyan accent

    // ── Light palette (mode terang) ───────────────────────────────────────────
    private val LIGHT_BG             = Color.parseColor("#EEF2F8")   // slate sangat muda
    private val LIGHT_CARD           = Color.parseColor("#FFFFFF")   // putih
    private val LIGHT_HEADER         = Color.parseColor("#FFFFFF")   // app bar flat putih
    private val LIGHT_BTN            = Color.parseColor("#0EA5E9")   // cyan accent (samakan dgn dark)
    private val LIGHT_TEXT_PRIMARY   = Color.parseColor("#0F172A")
    private val LIGHT_TEXT_SECONDARY = Color.parseColor("#64748B")
    private val LIGHT_ICON_TINT      = Color.parseColor("#0EA5E9")   // cyan accent

    // ── BottomNav ─────────────────────────────────────────────────────────────
    private val DARK_NAV_BG          = Color.parseColor("#142869")
    private val LIGHT_NAV_BG         = Color.parseColor("#FFFFFF")

    // ─────────────────────────────────────────────────────────────────────────

    fun isDarkMode(context: Context): Boolean {
        val prefs: SharedPreferences =
            context.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(SettingsActivity.KEY_DARK_MODE, false)
    }

    /** Terapkan theme ke seluruh view hierarchy dalam activity. */
    fun applyTheme(activity: Activity, rootView: View) {
        val dark = isDarkMode(activity)

        // ✅ FIX 1: Background root selalu di-set — mencakup ScrollView & CoordinatorLayout
        rootView.setBackgroundColor(if (dark) DARK_BG else LIGHT_BG)

        applyToView(rootView, dark)
    }

    // ── Rekursif ke semua child ───────────────────────────────────────────────

    private fun applyToView(view: View, dark: Boolean) {
        when (view) {
            is BottomNavigationView -> applyBottomNav(view, dark)
            is CardView             -> applyCard(view, dark)
            // TextInputLayout harus dicek SEBELUM LinearLayout karena ia extend ViewGroup
            is TextInputLayout      -> applyTextInputLayout(view, dark)
            is TextInputEditText    -> applyTextInputEditText(view, dark)
            is MaterialButton       -> applyMaterialButton(view, dark)
            is Button               -> { /* Button biasa — skip, sudah ter-cover MaterialButton */ }
            is SwitchMaterial       -> { /* biarkan sistem */ }
            is ImageView            -> applyImageView(view, dark)
            is TextView             -> {
                applyTextView(view, dark)
                // ✅ FIX: TextView yang punya background warna header (mis. tv_header_title di DetectionLog)
                applyViewBackground(view, dark)
            }
            // ✅ Wrapper scroll/halaman utama → boleh dipaksa warna dasar kalau belum punya background.
            is ScrollView,
            is androidx.core.widget.NestedScrollView,
            is androidx.coordinatorlayout.widget.CoordinatorLayout ->
                applyContainer(view, dark, forceFillIfNull = true)
            // ✅ Container pengelompokan biasa (baris judul, dll) → JANGAN dipaksa kalau memang
            //    sengaja transparan, supaya warna parent (mis. header navy) tetap terlihat,
            //    bukan ditimpa warna dasar gelap (dulu nyaris tak kentara, sekarang kontras & jelek).
            is androidx.constraintlayout.widget.ConstraintLayout,
            is FrameLayout,
            is LinearLayout         -> applyContainer(view, dark, forceFillIfNull = false)
        }

        // Rekursif ke children
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyToView(view.getChildAt(i), dark)
            }
        }
    }

    /**
     * Terapkan theme ke background sebuah view (termasuk TextView yang punya background warna).
     * Digunakan untuk header seperti tv_header_title di DetectionLogActivity.
     */
    private fun applyViewBackground(view: View, dark: Boolean) {
        val bg = view.background ?: return
        if (bg !is android.graphics.drawable.ColorDrawable) return
        val c = bg.color
        if (Color.alpha(c) == 0) return
        when {
            isSimilar(c, DARK_HEADER) || isSimilar(c, LIGHT_HEADER) -> {
                view.setBackgroundColor(if (dark) DARK_HEADER else LIGHT_HEADER)
                // ✅ Header dark = panel navy gelap (teks putih kontras).
                //    Header light = app bar flat putih (teks harus gelap, bukan putih).
                if (view is TextView) view.setTextColor(if (dark) DARK_TEXT_PRIMARY else LIGHT_TEXT_PRIMARY)
            }
            isSimilar(c, DARK_BG)    || isSimilar(c, LIGHT_BG) ->
                view.setBackgroundColor(if (dark) DARK_BG else LIGHT_BG)
            isSimilar(c, DARK_CARD)  || isSimilar(c, LIGHT_CARD) ->
                view.setBackgroundColor(if (dark) DARK_CARD else LIGHT_CARD)
            isSimilar(c, DARK_BTN)   || isSimilar(c, LIGHT_BTN) ->
                view.setBackgroundColor(if (dark) DARK_BTN else LIGHT_BTN)
        }
    }

    private fun applyContainer(view: View, dark: Boolean, forceFillIfNull: Boolean) {
        val bg = view.background
        val targetBg = if (dark) DARK_BG else LIGHT_BG

        when {
            // Tidak ada background:
            // - wrapper scroll/halaman utama → boleh diisi warna dasar (forceFillIfNull=true)
            // - container pengelompokan biasa → biarkan transparan, mewarisi warna parent
            bg == null -> if (forceFillIfNull) view.setBackgroundColor(targetBg)

            bg is android.graphics.drawable.ColorDrawable -> {
                val c = bg.color
                // Transparan (alpha = 0) → tetap transparan, jangan diubah
                if (Color.alpha(c) == 0) return
                when {
                    isSimilar(c, DARK_BG)     || isSimilar(c, LIGHT_BG)
                        -> view.setBackgroundColor(if (dark) DARK_BG else LIGHT_BG)
                    isSimilar(c, DARK_CARD)   || isSimilar(c, LIGHT_CARD)
                        -> view.setBackgroundColor(if (dark) DARK_CARD else LIGHT_CARD)
                    isSimilar(c, DARK_HEADER) || isSimilar(c, LIGHT_HEADER)
                        -> view.setBackgroundColor(if (dark) DARK_HEADER else LIGHT_HEADER)
                    isSimilar(c, DARK_BTN)    || isSimilar(c, LIGHT_BTN)
                        -> view.setBackgroundColor(if (dark) DARK_BTN else LIGHT_BTN)
                    isSimilar(c, DARK_NAV_BG) || isSimilar(c, LIGHT_NAV_BG)
                        -> view.setBackgroundColor(if (dark) DARK_NAV_BG else LIGHT_NAV_BG)
                    // Warna lain yang tidak dikenal → set ke bg utama agar tidak stuck putih
                    else -> view.setBackgroundColor(targetBg)
                }
            }
            // Drawable kompleks (selector, shape, dll) → jangan diubah paksa
            else -> { }
        }
    }

    private fun applyCard(view: CardView, dark: Boolean) {
        view.setCardBackgroundColor(if (dark) DARK_CARD else LIGHT_CARD)
        // Rekursif ke children
        for (i in 0 until view.childCount) applyToView(view.getChildAt(i), dark)
    }

    private fun applyBottomNav(view: BottomNavigationView, dark: Boolean) {
        val bgColor  = if (dark) DARK_NAV_BG else LIGHT_NAV_BG
        val iconTint = if (dark) DARK_TEXT_PRIMARY else LIGHT_TEXT_PRIMARY

        // Background bar keseluruhan
        view.setBackgroundColor(bgColor)
        view.itemIconTintList = android.content.res.ColorStateList.valueOf(iconTint)
        view.itemTextColor    = android.content.res.ColorStateList.valueOf(iconTint)

        // ✅ FIX ANIMASI NAIK: Paksa unlabeled agar icon tidak bergerak naik saat dipilih
        view.labelVisibilityMode = com.google.android.material.bottomnavigation.LabelVisibilityMode.LABEL_VISIBILITY_UNLABELED

        // ✅ FIX: Set itemBackground null agar tidak ada drawable apapun per-item
        view.itemBackground = null

        // ✅ FIX: Active indicator color (Material3 "pill" di belakang icon aktif)
        try {
            view.itemActiveIndicatorColor = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.TRANSPARENT
            )
        } catch (_: Exception) { }

        // ✅ FIX EXTRA: Hapus background putih dari child item view
        try {
            val menuView = view.getChildAt(0) as? ViewGroup ?: return
            for (i in 0 until menuView.childCount) {
                val itemView = menuView.getChildAt(i)
                itemView.background = null
                if (itemView is ViewGroup) {
                    for (j in 0 until itemView.childCount) {
                        val child = itemView.getChildAt(j)
                        val childBg = child.background
                        if (childBg is android.graphics.drawable.ColorDrawable) {
                            if (isSimilar(childBg.color, android.graphics.Color.WHITE)) {
                                child.background = null
                            }
                        } else if (childBg != null) {
                            child.backgroundTintList = android.content.res.ColorStateList.valueOf(
                                android.graphics.Color.TRANSPARENT
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) { }
    }

    private fun applyMaterialButton(view: MaterialButton, dark: Boolean) {
        // Tombol khusus warna merah (offline / batal) — jangan diubah
        val bg = view.backgroundTintList?.defaultColor ?: 0
        val isRed    = isSimilar(bg, Color.parseColor("#E53935")) ||
                isSimilar(bg, Color.parseColor("#F44336"))
        val isGreen  = isSimilar(bg, Color.parseColor("#4CAF50"))
        val isGrey   = isSimilar(bg, Color.parseColor("#455A64"))
        if (isRed || isGreen) return   // biarkan warna fungsional
        if (isGrey) {
            view.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (dark) Color.parseColor("#455A64") else Color.parseColor("#78909C")
            )
            return
        }
        view.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (dark) DARK_BTN else LIGHT_BTN
        )
        view.setTextColor(Color.WHITE)
    }

    private fun applyButton(view: Button, dark: Boolean) {
        // Hanya MaterialButton yang bukan Material — jarang, skip jika ada
    }

    private fun applyTextInputLayout(view: TextInputLayout, dark: Boolean) {
        // ✅ FIX: Warna yang benar-benar kontras untuk tiap mode
        val hintColor  = if (dark) DARK_TEXT_SECONDARY else LIGHT_TEXT_SECONDARY
        val strokeColor = if (dark) DARK_ICON_TINT else LIGHT_ICON_TINT  // biru kontras di kedua mode

        // Hint (label mengambang di atas field)
        view.defaultHintTextColor = android.content.res.ColorStateList.valueOf(hintColor)
        view.hintTextColor = android.content.res.ColorStateList.valueOf(
            if (dark) DARK_ICON_TINT else LIGHT_ICON_TINT   // biru saat focused
        )

        // ✅ FIX: Border/stroke — di light mode pakai biru gelap agar terlihat di card putih
        val strokeStateList = android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_focused),
                intArrayOf(-android.R.attr.state_focused)
            ),
            intArrayOf(
                if (dark) DARK_ICON_TINT else LIGHT_ICON_TINT,       // focused
                if (dark) Color.parseColor("#3D5A99") else Color.parseColor("#90CAF9")  // unfocused
            )
        )
        view.setBoxStrokeColorStateList(strokeStateList)

        // Background field (kotak dalam) — transparan agar ikut card
        view.boxBackgroundColor = if (dark) Color.TRANSPARENT else Color.TRANSPARENT
    }

    private fun applyTextInputEditText(view: TextInputEditText, dark: Boolean) {
        // ✅ FIX: Teks yang diketik harus kontras di kedua mode
        view.setTextColor(if (dark) DARK_TEXT_PRIMARY else LIGHT_TEXT_PRIMARY)
        view.setHintTextColor(if (dark) DARK_TEXT_SECONDARY else LIGHT_TEXT_SECONDARY)
    }

    private fun applyImageView(view: ImageView, dark: Boolean) {
        // Terapkan tint hanya jika memang sudah diberi tint sebelumnya (bukan foto profil)
        if (view.id == R.id.iv_profile_photo) return
        val tint = view.imageTintList?.defaultColor ?: return
        when {
            isSimilar(tint, Color.parseColor("#64B5F6")) ||   // text_cyan dark
                    isSimilar(tint, Color.parseColor("#1565C0")) ||   // LIGHT_ICON_TINT
                    isSimilar(tint, DARK_ICON_TINT) ->
                view.setColorFilter(if (dark) DARK_ICON_TINT else LIGHT_ICON_TINT)
            // Status merah/hijau — jangan diubah
        }
    }

    private fun applyTextView(view: TextView, dark: Boolean) {
        val c = view.currentTextColor

        // ✅ Teks aksen/link (cyan brand, mis. badge "AI ACTIVE", link "Daftar Sekarang")
        // dipertahankan sebagai aksen, bukan diratakan jadi warna teks primer.
        val accentLike = isSimilar(c, DARK_ICON_TINT, tolerance = 20) ||
                isSimilar(c, LIGHT_ICON_TINT, tolerance = 20) ||
                isSimilar(c, Color.parseColor("#64B5F6"), tolerance = 20)
        if (accentLike) {
            view.setTextColor(if (dark) DARK_ICON_TINT else LIGHT_ICON_TINT)
            return
        }

        // ✅ PRIORITAS UTAMA: cek background milik TextView itu sendiri
        val bg = view.background
        if (bg is android.graphics.drawable.ColorDrawable && Color.alpha(bg.color) != 0) {
            if (isSimilar(bg.color, DARK_HEADER) || isSimilar(bg.color, LIGHT_HEADER) ||
                isSimilar(bg.color, DARK_BG)     || isSimilar(bg.color, DARK_CARD)    ||
                isSimilar(bg.color, DARK_BTN)) {
                view.setTextColor(if (dark) DARK_TEXT_PRIMARY else LIGHT_TEXT_PRIMARY)
                return
            }
        }

        // ✅ PRIORITAS KEDUA: cek background parent — kasus "DRONE MONITORING" di MainActivity
        // TextView-nya transparan tapi parent LinearLayout-nya biru header
        val parentBg = (view.parent as? android.view.View)?.background
        if (parentBg is android.graphics.drawable.ColorDrawable && Color.alpha(parentBg.color) != 0) {
            if (isSimilar(parentBg.color, DARK_HEADER) || isSimilar(parentBg.color, LIGHT_HEADER) ||
                isSimilar(parentBg.color, DARK_BG)     || isSimilar(parentBg.color, DARK_CARD)    ||
                isSimilar(parentBg.color, DARK_BTN)) {
                view.setTextColor(if (dark) DARK_TEXT_PRIMARY else LIGHT_TEXT_PRIMARY)
                return
            }
        }

        // ── Warna status fungsional — JANGAN diubah ────────────────────────────
        val functionalColors = listOf(
            "#F44336", "#E53935", "#EF4444",   // merah (error/offline/severity tinggi)
            "#4CAF50", "#22C55E",              // hijau (connected/live/severity rendah)
            "#FF9800", "#F59E0B"               // oranye/amber (menghubungkan/severity sedang)
        ).map { Color.parseColor(it) }
        if (functionalColors.any { isSimilar(c, it, tolerance = 20) }) return

        // ── Semua teks lainnya: tentukan target warna berdasarkan "gelap/terang"-nya warna saat ini
        val isDarkColor = isDarkColor(c)

        when {
            // Teks sekunder / placeholder (#80FFFFFF, #B0BEC5, #455A64, abu-abu)
            isSimilar(c, Color.parseColor("#B0BEC5")) ||
                    isSimilar(c, Color.parseColor("#455A64")) ||
                    isSimilar(c, Color.parseColor("#90A4AE")) ||
                    Color.alpha(c) < 200 ->   // semi-transparan → pasti teks sekunder
                view.setTextColor(if (dark) DARK_TEXT_SECONDARY else LIGHT_TEXT_SECONDARY)

            // ✅ FIX: Warna terang (putih / biru muda) → teks primer di dark mode
            !isDarkColor ->
                view.setTextColor(if (dark) DARK_TEXT_PRIMARY else LIGHT_TEXT_PRIMARY)

            // ✅ FIX: Warna gelap (hitam / biru tua) → teks primer di light mode
            isDarkColor ->
                view.setTextColor(if (dark) DARK_TEXT_PRIMARY else LIGHT_TEXT_PRIMARY)
        }
    }

    /**
     * Cek apakah warna tergolong "gelap" berdasarkan luminance.
     * Putih = luminance tinggi = bukan gelap.
     * Hitam = luminance rendah = gelap.
     */
    private fun isDarkColor(color: Int): Boolean {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        // Rumus luminance relatif (WCAG)
        val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
        return luminance < 0.5
    }

    // ── Utility: apakah dua warna "mirip" (toleransi 30 per channel) ──────────
    private fun isSimilar(c1: Int, c2: Int, tolerance: Int = 30): Boolean {
        return Math.abs(Color.red(c1) - Color.red(c2)) <= tolerance &&
                Math.abs(Color.green(c1) - Color.green(c2)) <= tolerance &&
                Math.abs(Color.blue(c1) - Color.blue(c2)) <= tolerance
    }
}