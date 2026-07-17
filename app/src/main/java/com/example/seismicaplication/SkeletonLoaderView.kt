package com.example.seismicaplication

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * SkeletonLoaderView
 * Placeholder shimmer ala "skeleton screen" untuk list deteksi yang sedang loading —
 * meniru bentuk item_detection_log.xml (thumbnail + 2 baris teks + chip) supaya tidak
 * ada lompatan layout saat data asli muncul. Murni Canvas, tanpa library tambahan.
 */
class SkeletonLoaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(value: Float) = value * density

    private val rowCount = 4
    private val rowHeight = dp(88f)
    private val rowSpacing = dp(12f)
    private val sidePadding = dp(16f)

    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val blockPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cardRect = RectF()

    private var shimmerProgress = 0f
    private var animator: ValueAnimator? = null

    init {
        setWillNotDraw(false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator?.cancel()
        animator = ValueAnimator.ofFloat(-1f, 2f).apply {
            duration = 1100L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                shimmerProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = (rowCount * (rowHeight + rowSpacing)).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val dark = ThemeManager.isDarkMode(context)
        val cardColor      = if (dark) Color.parseColor("#141B2E") else Color.parseColor("#FFFFFF")
        val blockColor     = if (dark) Color.parseColor("#1E2A47") else Color.parseColor("#E2E8F0")
        val highlightColor = if (dark) Color.parseColor("#2AFFFFFF") else Color.parseColor("#80FFFFFF")

        val w = width.toFloat()
        var top = 0f

        for (i in 0 until rowCount) {
            cardPaint.color = cardColor
            cardRect.set(sidePadding, top, w - sidePadding, top + rowHeight)
            canvas.drawRoundRect(cardRect, dp(14f), dp(14f), cardPaint)

            blockPaint.color = blockColor

            // Thumbnail placeholder
            val thumbSize = rowHeight - dp(24f)
            val thumbLeft = sidePadding + dp(12f)
            val thumbTop  = top + dp(12f)
            canvas.drawRoundRect(
                thumbLeft, thumbTop, thumbLeft + thumbSize, thumbTop + thumbSize,
                dp(10f), dp(10f), blockPaint
            )

            // Baris teks placeholder
            val lineLeft = thumbLeft + thumbSize + dp(14f)
            val line1Top = thumbTop + dp(8f)
            canvas.drawRoundRect(
                lineLeft, line1Top, lineLeft + w * 0.28f, line1Top + dp(12f),
                dp(6f), dp(6f), blockPaint
            )
            val line2Top = line1Top + dp(22f)
            canvas.drawRoundRect(
                lineLeft, line2Top, lineLeft + w * 0.18f, line2Top + dp(10f),
                dp(5f), dp(5f), blockPaint
            )

            // Chip placeholder (kanan, ala badge confidence)
            val chipW = dp(56f)
            val chipH = dp(26f)
            val chipLeft = w - sidePadding - dp(12f) - chipW
            val chipTop = top + (rowHeight - chipH) / 2f
            canvas.drawRoundRect(
                chipLeft, chipTop, chipLeft + chipW, chipTop + chipH,
                chipH / 2f, chipH / 2f, blockPaint
            )

            top += rowHeight + rowSpacing
        }

        // Sapuan shimmer diagonal-ish (horizontal) di atas semua baris
        val sweepWidth = w * 0.6f
        val sweepX = shimmerProgress * (w + sweepWidth) - sweepWidth
        val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                sweepX, 0f, sweepX + sweepWidth, 0f,
                intArrayOf(Color.TRANSPARENT, highlightColor, Color.TRANSPARENT),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, top, shimmerPaint)
    }
}
