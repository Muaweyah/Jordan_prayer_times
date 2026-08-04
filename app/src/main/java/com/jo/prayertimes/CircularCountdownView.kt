package com.jo.prayertimes

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View

/**
 * حلقة دائرية تعرض نسبة تقدم الوقت المتبقي حتى الصلاة القادمة،
 * على شكل قوس بزاوية 270 درجة يبدأ ويحيط بأسفل الدائرة كفجوة.
 */
class CircularCountdownView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val startAngle = 180f
    private val sweepTotal = 180f
    private var progress = 0f // من 0 إلى 1

    private val strokeWidthPx = 5f * resources.displayMetrics.density
    private val arcRect = RectF()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#33415C")
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
    }

    fun setProgress(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        if (clamped != progress) {
            progress = clamped
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // نبني قوساً نصف دائري حقيقي (نصف قطره متساوٍ أفقياً وعمودياً)، قاعدته المسطحة
        // ترتكز تماماً عند الحافة السفلية للعنصر، لتلامس مباشرة العنصر الذي يليه أسفله
        val inset = strokeWidthPx / 2f + 2f
        val radius = (w / 2f) - inset
        val centerX = w / 2f
        val bottomY = h.toFloat() - inset
        arcRect.set(centerX - radius, bottomY - 2f * radius, centerX + radius, bottomY)

        val colors = intArrayOf(
            Color.parseColor("#2FD1BE"),
            Color.parseColor("#34D399"),
            Color.parseColor("#2FD1BE")
        )
        val gradient = SweepGradient(w / 2f, h / 2f, colors, null)
        val matrix = Matrix()
        matrix.postRotate(startAngle, w / 2f, h / 2f)
        gradient.setLocalMatrix(matrix)
        progressPaint.shader = gradient
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawArc(arcRect, startAngle, sweepTotal, false, trackPaint)
        if (progress > 0f) {
            canvas.drawArc(arcRect, startAngle, sweepTotal * progress, false, progressPaint)
        }
    }
}
