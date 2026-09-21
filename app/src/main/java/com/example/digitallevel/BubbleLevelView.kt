package com.example.digitallevel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class BubbleLevelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }

    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private var pitch: Float = 0f // In degrees
    private var roll: Float = 0f  // In degrees

    fun updateOrientation(pitch: Float, roll: Float) {
        this.pitch = pitch
        this.roll = roll
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(centerX, centerY) * 0.9f
        val bubbleRadius = radius * 0.2f

        // Draw background circle
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)
        canvas.drawCircle(centerX, centerY, radius, borderPaint)

        // Draw crosshairs
        canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, crosshairPaint)
        canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, crosshairPaint)
        canvas.drawCircle(centerX, centerY, radius * 0.1f, crosshairPaint)

        // Calculate bubble position based on pitch and roll
        // Max tilt assumed to be around 45 degrees for full displacement
        val maxTilt = 45f
        val displacementX = (roll / maxTilt).coerceIn(-1f, 1f) * (radius - bubbleRadius)
        val displacementY = (pitch / maxTilt).coerceIn(-1f, 1f) * (radius - bubbleRadius)

        canvas.drawCircle(centerX + displacementX, centerY - displacementY, bubbleRadius, bubblePaint)
    }
}
