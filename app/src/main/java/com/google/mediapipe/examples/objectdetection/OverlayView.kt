/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mediapipe.examples.objectdetection

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult
import kotlin.math.max
import kotlin.math.min

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: ObjectDetectorResult? = null
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()
    private var scaleFactor: Float = 1f
    private var bounds = Rect()
    private var outputWidth = 0
    private var outputHeight = 0
    private var outputRotate = 0
    private var runningMode: RunningMode = RunningMode.IMAGE

    // --- New state for object selection and tracking ---
    private var selectedDetectionIndex: Int? = null
    private var lastSelectedCenter: PointF? = null
    private var lastSelectedTimestampNanos: Long = 0
    private val trackingTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        style = Paint.Style.FILL
    }
    private val trackingBackgroundPaint = Paint().apply {
        color = Color.BLACK
        alpha = 180
        style = Paint.Style.FILL
    }
    private val stopButtonPaint = Paint().apply {
        style = Paint.Style.FILL
    }
    private val stopButtonTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private var isStopButtonPressed = false
    // ---------------------------------------------------

    init {
        initPaints()
    }

    fun clear() {
        results = null
        selectedDetectionIndex = null
        lastSelectedCenter = null
        lastSelectedTimestampNanos = 0
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    fun setRunningMode(runningMode: RunningMode) {
        this.runningMode = runningMode
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.mp_primary)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE

        stopButtonPaint.color = ContextCompat.getColor(context!!, R.color.mp_primary)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.detections()?.map {
            val boxRect = RectF(
                it.boundingBox().left,
                it.boundingBox().top,
                it.boundingBox().right,
                it.boundingBox().bottom
            )
            val matrix = Matrix()
            matrix.postTranslate(-outputWidth / 2f, -outputHeight / 2f)
            matrix.postRotate(outputRotate.toFloat())

            // If the outputRotate is 90 or 270 degrees, the translation is
            // applied after the rotation. This is because a 90 or 270 degree rotation
            // flips the image vertically or horizontally, respectively.
            if (outputRotate == 90 || outputRotate == 270) {
                matrix.postTranslate(outputHeight / 2f, outputWidth / 2f)
            } else {
                matrix.postTranslate(outputWidth / 2f, outputHeight / 2f)
            }
            matrix.mapRect(boxRect)
            boxRect
        }?.forEachIndexed { index, floats ->

            val top = floats.top * scaleFactor
            val bottom = floats.bottom * scaleFactor
            val left = floats.left * scaleFactor
            val right = floats.right * scaleFactor

            // Draw bounding box around detected objects
            val drawableRect = RectF(left, top, right, bottom)
            if (selectedDetectionIndex == index) {
                // Draw selected object in green
                boxPaint.color = ContextCompat.getColor(context!!, R.color.mp_green_500)
                canvas.drawRect(drawableRect, boxPaint)
                boxPaint.color = ContextCompat.getColor(context!!, R.color.mp_primary) // Reset
            } else {
                canvas.drawRect(drawableRect, boxPaint)
            }

            // Create text to display alongside detected objects
            val category = results?.detections()!![index].categories()[0]
            val drawableText =
                category.categoryName() + " " + String.format(
                    "%.2f",
                    category.score()
                )

            // Draw rect behind display text
            textBackgroundPaint.getTextBounds(
                drawableText,
                0,
                drawableText.length,
                bounds
            )
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            canvas.drawRect(
                left,
                top,
                left + textWidth + BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )

            // Draw text for detected object
            canvas.drawText(
                drawableText,
                left,
                top + bounds.height(),
                textPaint
            )
        }

        // --- Draw tracking data and stop button if an object is selected ---
        selectedDetectionIndex?.let { index ->
            val detections = results?.detections()
            if (detections != null && index < detections.size) {
                val detection = detections[index]
                val boxRect = RectF(
                    detection.boundingBox().left,
                    detection.boundingBox().top,
                    detection.boundingBox().right,
                    detection.boundingBox().bottom
                )
                val matrix = Matrix()
                matrix.postTranslate(-outputWidth / 2f, -outputHeight / 2f)
                matrix.postRotate(outputRotate.toFloat())
                if (outputRotate == 90 || outputRotate == 270) {
                    matrix.postTranslate(outputHeight / 2f, outputWidth / 2f)
                } else {
                    matrix.postTranslate(outputWidth / 2f, outputHeight / 2f)
                }
                matrix.mapRect(boxRect)

                val centerX = (boxRect.left + boxRect.right) / 2
                val centerY = (boxRect.top + boxRect.bottom) / 2
                val currentCenter = PointF(centerX * scaleFactor, centerY * scaleFactor)

                // Calculate velocity if we have a previous center
                val nowNanos = System.nanoTime()
                var velocityText = ""
                lastSelectedCenter?.let { lastCenter ->
                    val timeDeltaSec = (nowNanos - lastSelectedTimestampNanos) / 1_000_000_000f
                    if (timeDeltaSec > 0) {
                        val vx = (currentCenter.x - lastCenter.x) / timeDeltaSec
                        val vy = (currentCenter.y - lastCenter.y) / timeDeltaSec
                        velocityText = String.format("vx: %.1f vy: %.1f px/s", vx, vy)
                    }
                }
                // Update last center and timestamp for next frame
                lastSelectedCenter = currentCenter
                lastSelectedTimestampNanos = nowNanos

                // Position text (we'll show x, y in view coordinates)
                val positionText = String.format("x: %.1f y: %.1f", currentCenter.x, currentCenter.y)

                // Draw background for tracking data (top-left corner)
                val trackingText = "Pos: $positionText\n$velocityText"
                trackingBackgroundPaint.getTextBounds(trackingText, 0, trackingText.length, bounds)
                val trackingWidth = bounds.width()
                val trackingHeight = bounds.height()
                val trackingMargin = 24f
                val trackingPadding = 12f
                canvas.drawRoundRect(
                    trackingMargin,
                    trackingMargin,
                    trackingMargin + trackingWidth + trackingPadding * 2,
                    trackingMargin + trackingHeight + trackingPadding * 2,
                    8f, 8f,
                    trackingBackgroundPaint
                )
                // Draw tracking text
                var y = trackingMargin + trackingPadding + trackingTextPaint.textSize
                trackingBackgroundPaint.getTextBounds("Pos: $positionText", 0, "Pos: $positionText".length, bounds)
                val lineHeight = bounds.height()
                canvas.drawText("Pos: $positionText", trackingMargin + trackingPadding, y, trackingTextPaint)
                y += lineHeight
                if (velocityText.isNotEmpty()) {
                    canvas.drawText(velocityText, trackingMargin + trackingPadding, y, trackingTextPaint)
                }

                // Draw stop button (top-right corner)
                val stopButtonWidth = 180f
                val stopButtonHeight = 50f
                val stopButtonMargin = 24f
                val stopButtonLeft = width - stopButtonWidth - stopButtonMargin
                val stopButtonTop = stopButtonMargin
                val stopButtonRect = RectF(stopButtonLeft, stopButtonTop,
                        stopButtonLeft + stopButtonWidth, stopButtonTop + stopButtonHeight)
                // Draw button with rounded corners
                // Use pressed color if pressed
                if (isStopButtonPressed) {
                    stopButtonPaint.color = ContextCompat.getColor(context!!, R.color.mp_variant)
                } else {
                    stopButtonPaint.color = ContextCompat.getColor(context!!, R.color.mp_primary)
                }
                canvas.drawRoundRect(stopButtonRect, 12f, 12f, stopButtonPaint)
                val stopButtonText = "Stop Tracking"
                stopButtonTextPaint.getTextBounds(stopButtonText, 0, stopButtonText.length, bounds)
                val textX = stopButtonLeft + stopButtonWidth / 2f
                val textY = stopButtonTop + stopButtonHeight / 2f + bounds.height() / 2f
                canvas.drawText(stopButtonText, textX, textY, stopButtonTextPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (results == null || results?.detections()?.isEmpty() == true) {
            return super.onTouchEvent(event)
        }

        val detections = results?.detections()
        val action = event.actionMasked
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
            val touchX = event.x
            val touchY = event.y

            // Check if stop button was pressed
            val stopButtonWidth = 180f
            val stopButtonHeight = 50f
            val stopButtonMargin = 24f
            val stopButtonLeft = width - stopButtonWidth - stopButtonMargin
            val stopButtonTop = stopButtonMargin
            if (touchX >= stopButtonLeft && touchX <= stopButtonLeft + stopButtonWidth &&
                    touchY >= stopButtonTop && touchY <= stopButtonTop + stopButtonHeight) {
                if (action == MotionEvent.ACTION_DOWN) {
                    isStopButtonPressed = true
                    invalidate()
                    return true
                } else if (action == MotionEvent.ACTION_UP) {
                    isStopButtonPressed = false
                    // Clear selection
                    selectedDetectionIndex = null
                    lastSelectedCenter = null
                    lastSelectedTimestampNanos = 0
                    invalidate()
                    return true
                }
            }

            // Check if any detection was tapped
            detections?.forEachIndexed { index, detection ->
                val boxRect = RectF(
                    detection.boundingBox().left,
                    detection.boundingBox().top,
                    detection.boundingBox().right,
                    detection.boundingBox().bottom
                )
                val matrix = Matrix()
                matrix.postTranslate(-outputWidth / 2f, -outputHeight / 2f)
                matrix.postRotate(outputRotate.toFloat())
                if (outputRotate == 90 || outputRotate == 270) {
                    matrix.postTranslate(outputHeight / 2f, outputWidth / 2f)
                } else {
                    matrix.postTranslate(outputWidth / 2f, outputHeight / 2f)
                }
                matrix.mapRect(boxRect)

                if (touchX >= boxRect.left && touchX <= boxRect.right &&
                        touchY >= boxRect.top && touchY <= boxRect.bottom) {
                    if (action == MotionEvent.ACTION_UP) {
                        selectedDetectionIndex = index
                        // Reset tracking state for new selection
                        lastSelectedCenter = null
                        lastSelectedTimestampNanos = 0
                        invalidate()
                    }
                    return true
                }
            }

            // If we get here, tap was outside any detection and not on stop button
            // Clear selection on tap up
            if (action == MotionEvent.ACTION_UP) {
                selectedDetectionIndex = null
                lastSelectedCenter = null
                lastSelectedTimestampNanos = 0
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun setResults(
        detectionResults: ObjectDetectorResult,
        outputHeight: Int,
        outputWidth: Int,
        imageRotation: Int
    ) {
        results = detectionResults
        this.outputWidth = outputWidth
        this.outputHeight = outputHeight
        this.outputRotate = imageRotation

        // Calculates the new width and height of an image after it has been rotated.
        // If `imageRotation` is 0 or 180, the new width and height are the same
        // as the original width and height.
        // If `imageRotation` is 90 or 270, the new width and height are swapped.
        val rotatedWidthHeight = when (imageRotation) {
            0, 180 -> Pair(outputWidth, outputHeight)
            90, 270 -> Pair(outputHeight, outputWidth)
            else -> return
        }

        // Images, videos are displayed in FIT_START mode.
        // Camera live streams is displayed in FILL_START mode. So we need to scale
        // up the bounding box to match with the size that the images/videos/live streams being
        // displayed.
        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(
                    width * 1f / rotatedWidthHeight.first,
                    height * 1f / rotatedWidthHeight.second
                )
            }

            RunningMode.LIVE_STREAM -> {
                max(
                    width * 1f / rotatedWidthHeight.first,
                    height * 1f / rotatedWidthHeight.second
                )
            }
        }

        invalidate()
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}