package com.example.seismicaplication

import android.animation.ObjectAnimator
import android.animation.AnimatorSet
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.cardview.widget.CardView

/**
 * MascotView — floating chibi character yang muncul di semua halaman.
 *
 * Cara ganti gambar karakter:
 *   Taruh file gambar kamu di res/drawable/ dengan nama: ic_mascot.png
 *   Ukuran ideal: 200x200px sampai 400x400px, format PNG transparan
 */
class MascotView(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // ── Root view maskot ──────────────────────────────────────────────────────
    private val mascotContainer = FrameLayout(context)
    private val mascotImage     = ImageView(context)

    // ── Bubble menu ───────────────────────────────────────────────────────────
    private var bubblePopup: PopupWindow? = null
    private var isBubbleShowing = false

    // ── Animasi breathing ─────────────────────────────────────────────────────
    private var breathingAnimator: AnimatorSet? = null

    // ── WindowManager params ──────────────────────────────────────────────────
    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        // BARU
        gravity = Gravity.TOP or Gravity.END
        x = 124   // margin dari kiri
        y = 20   // margin dari atas
    }

    // ── Menu items ────────────────────────────────────────────────────────────
    private data class MenuItem(val icon: String, val label: String, val action: () -> Unit)

    private val menuItems: List<MenuItem> by lazy {
        listOf(
            MenuItem("🏠", "Dashboard")     { navigate(MainActivity::class.java) },
            MenuItem("📋", "Detection Log") { navigate(DetectionLogActivity::class.java) },
            MenuItem("📡", "Monitoring")    { navigate(NotificationActivity::class.java) },
            MenuItem("👤", "Profile")       { navigate(ProfileActivity::class.java) },
            MenuItem("⚙️", "Settings")      { navigate(SettingsActivity::class.java) },
            MenuItem("❓", "Help")          { showHelp() }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────

    fun attach() {
        setupMascotView()
        windowManager.addView(mascotContainer, params)
        startBreathingAnimation()
    }

    fun detach() {
        stopBreathingAnimation()
        dismissBubble()
        try { windowManager.removeView(mascotContainer) } catch (_: Exception) {}
    }

    // ── Setup view maskot ─────────────────────────────────────────────────────

    private fun setupMascotView() {
        val sizePx = dpToPx(30)

        mascotImage.layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)

        try {
            mascotImage.setImageResource(R.drawable.ic_mascot)
        } catch (_: Exception) {
            mascotImage.setImageResource(android.R.drawable.ic_dialog_info)
        }

        mascotImage.scaleType = ImageView.ScaleType.FIT_CENTER
        mascotContainer.addView(mascotImage)

        mascotContainer.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80).start()
            } else if (event.action == MotionEvent.ACTION_UP) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                v.performClick()
            }
            true
        }

        mascotContainer.setOnClickListener { toggleBubble() }
    }

    // ── Animasi breathing (naik turun pelan) ──────────────────────────────────

    private fun startBreathingAnimation() {
        val moveUp = ObjectAnimator.ofFloat(mascotContainer, "translationY", 0f, -14f).apply {
            duration = 1800
            interpolator = AccelerateDecelerateInterpolator()
        }
        val moveDown = ObjectAnimator.ofFloat(mascotContainer, "translationY", -14f, 0f).apply {
            duration = 1800
            interpolator = AccelerateDecelerateInterpolator()
        }
        val scaleUpX   = ObjectAnimator.ofFloat(mascotContainer, "scaleX", 1f, 1.04f).apply { duration = 1800 }
        val scaleUpY   = ObjectAnimator.ofFloat(mascotContainer, "scaleY", 1f, 1.04f).apply { duration = 1800 }
        val scaleDownX = ObjectAnimator.ofFloat(mascotContainer, "scaleX", 1.04f, 1f).apply { duration = 1800 }
        val scaleDownY = ObjectAnimator.ofFloat(mascotContainer, "scaleY", 1.04f, 1f).apply { duration = 1800 }

        val inhale = AnimatorSet().apply { playTogether(moveUp, scaleUpX, scaleUpY) }
        val exhale = AnimatorSet().apply { playTogether(moveDown, scaleDownX, scaleDownY) }

        breathingAnimator = AnimatorSet().apply {
            playSequentially(inhale, exhale)
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    start()
                }
            })
            start()
        }
    }

    private fun stopBreathingAnimation() {
        breathingAnimator?.cancel()
        breathingAnimator = null
    }

    // ── Bubble Menu ───────────────────────────────────────────────────────────

    private fun toggleBubble() {
        if (isBubbleShowing) dismissBubble() else showBubble()
    }

    private fun showBubble() {
        dismissBubble()
        stopBreathingAnimation()

        val bubbleView = buildBubbleView()

        bubblePopup = PopupWindow(
            bubbleView,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            setOnDismissListener {
                isBubbleShowing = false
                startBreathingAnimation()
            }
            showAtLocation(
                mascotContainer,
                Gravity.BOTTOM or Gravity.END,
                dpToPx(80),
                params.y + dpToPx(80)
            )
        }

        isBubbleShowing = true

        bubbleView.alpha = 0f
        bubbleView.scaleX = 0.7f
        bubbleView.scaleY = 0.7f
        bubbleView.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun buildBubbleView(): View {
        val padding = dpToPx(12)

        val card = CardView(context).apply {
            radius = dpToPx(16).toFloat()
            cardElevation = dpToPx(8).toFloat()
            setCardBackgroundColor(
                if (ThemeManager.isDarkMode(context))
                    android.graphics.Color.parseColor("#0D2560")
                else
                    android.graphics.Color.WHITE
            )
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val tvGreet = TextView(context).apply {
            text = "✨ Mau ke mana?"
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(
                if (ThemeManager.isDarkMode(context))
                    android.graphics.Color.WHITE
                else
                    android.graphics.Color.parseColor("#0D1B40")
            )
            setPadding(0, 0, 0, dpToPx(8))
        }
        layout.addView(tvGreet)

        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)
            ).also { it.bottomMargin = dpToPx(8) }
            setBackgroundColor(android.graphics.Color.parseColor("#3D5A99"))
        }
        layout.addView(divider)

        menuItems.forEach { item ->
            val btn = TextView(context).apply {
                text = "${item.icon}  ${item.label}"
                textSize = 14f
                setTextColor(
                    if (ThemeManager.isDarkMode(context))
                        android.graphics.Color.WHITE
                    else
                        android.graphics.Color.parseColor("#0D1B40")
                )
                setPadding(dpToPx(4), dpToPx(10), dpToPx(24), dpToPx(10))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    dismissBubble()
                    item.action()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    foreground = context.obtainStyledAttributes(
                        intArrayOf(android.R.attr.selectableItemBackground)
                    ).getDrawable(0)
                }
            }
            layout.addView(btn)
        }

        card.addView(layout)
        return card
    }

    private fun dismissBubble() {
        try { bubblePopup?.dismiss() } catch (_: Exception) {}
        bubblePopup = null
        isBubbleShowing = false
    }

    // ── Navigasi ──────────────────────────────────────────────────────────────

    private fun <T> navigate(clazz: Class<T>) {
        val intent = Intent(context, clazz).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(intent)
    }

    private fun showHelp() {
        Toast.makeText(
            context,
            "💡 Tap ikon di menu bawah untuk berpindah halaman.\n" +
                    "📡 Monitoring: lihat grafik deteksi live.\n" +
                    "📋 Log: riwayat semua deteksi.",
            Toast.LENGTH_LONG
        ).show()
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private fun dpToPx(dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()
}