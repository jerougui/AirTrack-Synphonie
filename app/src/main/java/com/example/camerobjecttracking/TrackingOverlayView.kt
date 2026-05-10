package com.example.camerobjecttracking

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class TrackingOverlayView : View {

    private val trackingPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    private val centerPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        isAntiAlias = true
    }

    private var trackingRect: RectF? = null
    private var centerPointX: Float = 0f
    private var centerPointY: Float = 0f
    private var isTracking = false
    
    private var normalizedX: Float = 0f
    private var normalizedY: Float = 0f
    private var fps: Int = 0

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    fun updateTracking(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        centerX: Float,
        centerY: Float,
        normX: Float = -1f,
        normY: Float = -1f,
        currentFps: Int = 0
    ) {
        trackingRect = RectF(left, top, right, bottom)
        centerPointX = centerX
        centerPointY = centerY
        normalizedX = normX
        normalizedY = normY
        fps = currentFps
        isTracking = true
        postInvalidateOnAnimation()
    }

    fun setDebugInfo(normX: Float, normY: Float, fps: Int) {
        normalizedX = normX
        normalizedY = normY
        this.fps = fps
        if (isTracking) postInvalidateOnAnimation()
    }

    fun clearTracking() {
        isTracking = false
        trackingRect = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isTracking && trackingRect != null) {
            canvas.drawRect(trackingRect!!, trackingPaint)
            canvas.drawCircle(centerPointX, centerPointY, 10f, centerPaint)
        }

        if (normalizedX >= 0 && normalizedY >= 0) {
            val debugText = "x=%.2f y=%.2f FPS: %d".format(normalizedX, normalizedY, fps)
            canvas.drawText(debugText, 20f, height - 40f, textPaint)
        }
    }
}