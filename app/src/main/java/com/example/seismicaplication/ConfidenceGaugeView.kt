package com.example.seismicaplication

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.sin

/**
 * ConfidenceGaugeView
 * Custom View gauge/speedometer untuk menampilkan confidence rata-rata secara real-time.
 * Tidak memerlukan library tambahan — murni Canvas Android.
 *
 * Cara pakai di XML:
 *   <com.example.seismicaplication.ConfidenceGaugeView
 *       android:id="@+id/gauge_confidence"
 *       android:layout_width="match_parent"
 *       android:layout_height="220dp" />
 *
 * Cara update nilai dari Activity/Fragment:
 *   gaugeView.setConfidence(0.85f)   // nilai 0.0 - 1.0
 */
class ConfidenceGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── Nilai ──────────────────────────────────────────────────
    private var currentValue = 0f      // 0.0 – 1.0
    private var animatedValue = 0f     // nilai yang sedang dianimasikan
    private var animator: ValueAnimator? = null

    // ── Sudut arc: 150° kiri s/d 30° kanan (total 240°) ───────
    private val START_ANGLE = 150f
    private val SWEEP_ANGLE = 240f

    // ── Paint ──────────────────────────────────────────────────
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.WHITE
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#B0BEC5")
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#455A64")
        strokeWidth = 2f
    }

    // ── Gradient warna arc: merah → kuning → hijau ─────────────
    private val arcColors = intArrayOf(
        Color.parseColor("#F44336"),   // 0% — merah
        Color.parseColor("#FF9800"),   // 33% — oranye
        Color.parseColor("#FFEB3B"),   // 66% — kuning
        Color.parseColor("#4CAF50")    // 100% — hijau
    )
    private val arcPositions = floatArrayOf(0f, 0.33f, 0.66f, 1f)

    private val rectF = RectF()

    // ── Public API ─────────────────────────────────────────────

    /**
     * Set nilai confidence baru dengan animasi smooth.
     * @param value nilai 0.0 – 1.0
     * @param animate true = animasi, false = langsung
     */
    fun setConfidence(value: Float, animate: Boolean = true) {
        val clamped = value.coerceIn(0f, 1f)
        currentValue = clamped

        animator?.cancel()
        if (animate) {
            animator = ValueAnimator.ofFloat(animatedValue, clamped).apply {
                duration = 800L
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    this@ConfidenceGaugeView.animatedValue = (it.animatedValue as? Float) ?: this@ConfidenceGaugeView.animatedValue
                    invalidate()
                }
                start()
            }
        } else {
            animatedValue = clamped
            invalidate()
        }
    }

    fun getConfidence() = currentValue

    // ── Draw ───────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h * 0.56f                    // sedikit ke bawah agar jarum punya ruang
        val strokeWidth = w * 0.065f
        val radius = (minOf(w, h * 1.1f) / 2f) - strokeWidth - 8f

        // ── Track (background arc abu) ─────────────────────────
        trackPaint.strokeWidth = strokeWidth
        trackPaint.color = Color.parseColor("#1A2A4A")
        rectF.set(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(rectF, START_ANGLE, SWEEP_ANGLE, false, trackPaint)

        // ── Arc berwarna (gradient simulasi via SweepGradient) ─
        val sweepGradient = SweepGradient(cx, cy, arcColors, arcPositions)
        // Rotasi gradient supaya sejajar dengan arc START_ANGLE
        val matrix = Matrix()
        matrix.postRotate(START_ANGLE, cx, cy)
        sweepGradient.setLocalMatrix(matrix)

        arcPaint.strokeWidth = strokeWidth
        arcPaint.shader = sweepGradient
        val sweepDraw = SWEEP_ANGLE * animatedValue
        if (sweepDraw > 0f)
            canvas.drawArc(rectF, START_ANGLE, sweepDraw, false, arcPaint)

        // ── Tick marks (5 buah: 0, 25, 50, 75, 100%) ──────────
        val tickOuter = radius + strokeWidth * 0.1f
        val tickInner = radius - strokeWidth * 0.9f
        tickPaint.strokeWidth = 2f
        for (i in 0..4) {
            val fraction = i / 4f
            val angleDeg = START_ANGLE + SWEEP_ANGLE * fraction
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val cos = cos(angleRad).toFloat()
            val sin = sin(angleRad).toFloat()
            canvas.drawLine(
                cx + tickOuter * cos, cy + tickOuter * sin,
                cx + tickInner * cos, cy + tickInner * sin,
                tickPaint
            )
            // Label persentase di luar tick
            labelPaint.textSize = w * 0.032f
            val labelR = tickOuter + strokeWidth * 0.9f
            val lx = cx + labelR * cos
            val ly = cy + labelR * sin + labelPaint.textSize / 3f
            canvas.drawText("${(fraction * 100).toInt()}%", lx, ly, labelPaint)
        }

        // ── Jarum ──────────────────────────────────────────────
        val needleAngleDeg = START_ANGLE + SWEEP_ANGLE * animatedValue
        val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
        val needleLen = radius * 0.72f
        val needleBaseWidth = strokeWidth * 0.35f

        val tipX = cx + needleLen * cos(needleAngleRad).toFloat()
        val tipY = cy + needleLen * sin(needleAngleRad).toFloat()

        // Jarum: segitiga tipis
        val perpRad = needleAngleRad + Math.PI / 2
        val bx1 = cx + needleBaseWidth * cos(perpRad).toFloat()
        val by1 = cy + needleBaseWidth * sin(perpRad).toFloat()
        val bx2 = cx - needleBaseWidth * cos(perpRad).toFloat()
        val by2 = cy - needleBaseWidth * sin(perpRad).toFloat()

        val needlePath = Path().apply {
            moveTo(tipX, tipY)
            lineTo(bx1, by1)
            lineTo(bx2, by2)
            close()
        }
        needlePaint.color = Color.WHITE
        canvas.drawPath(needlePath, needlePaint)

        // Lingkaran tengah
        canvas.drawCircle(cx, cy, strokeWidth * 0.45f, centerDotPaint)
        centerDotPaint.color = Color.parseColor("#0D3B8E")
        canvas.drawCircle(cx, cy, strokeWidth * 0.25f, centerDotPaint)
        centerDotPaint.color = Color.WHITE  // reset

        // ── Nilai persentase di tengah ─────────────────────────
        val pct = (animatedValue * 100).toInt()
        textPaint.textSize = w * 0.14f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("$pct%", cx, cy + radius * 0.52f, textPaint)

        // ── Label status di bawah angka ────────────────────────
        val label = when {
            animatedValue >= 0.80f -> "SANGAT TINGGI"
            animatedValue >= 0.65f -> "TINGGI"
            animatedValue >= 0.50f -> "SEDANG"
            else                   -> "RENDAH"
        }
        val labelColor = when {
            animatedValue >= 0.80f -> Color.parseColor("#4CAF50")
            animatedValue >= 0.65f -> Color.parseColor("#8BC34A")
            animatedValue >= 0.50f -> Color.parseColor("#FFEB3B")
            else                   -> Color.parseColor("#F44336")
        }
        labelPaint.textSize = w * 0.04f
        labelPaint.color = labelColor
        canvas.drawText(label, cx, cy + radius * 0.52f + textPaint.textSize * 0.9f, labelPaint)
        labelPaint.color = Color.parseColor("#B0BEC5")  // reset
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}