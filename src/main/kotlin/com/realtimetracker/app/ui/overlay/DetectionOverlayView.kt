package com.realtimetracker.app.ui.overlay

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.realtimetracker.app.ml.YoloDetection
import com.realtimetracker.app.tracking.TrackingMetrics

class DetectionOverlayView(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var detections = listOf<YoloDetection>()
    private var metrics = mapOf<Int, TrackingMetrics>()
    private var brightness = 0f
    private var fps = 0

    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 24f
        isAntiAlias = true
    }

    private val metricsTextPaint = Paint().apply {
        color = Color.YELLOW
        textSize = 18f
        isAntiAlias = true
    }

    private val bgPaint = Paint().apply {
        color = Color.argb(150, 0, 0, 0)
    }

    fun updateDetections(newDetections: List<YoloDetection>) {
        detections = newDetections
        invalidate()
    }

    fun updateMetrics(newMetrics: Map<Int, TrackingMetrics>) {
        metrics = newMetrics
        invalidate()
    }

    fun updateBrightness(newBrightness: Float) {
        brightness = newBrightness
        invalidate()
    }

    fun updateFPS(newFps: Int) {
        fps = newFps
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw detection boxes
        for ((index, detection) in detections.withIndex()) {
            // Draw bounding box
            canvas.drawRect(
                detection.x1,
                detection.y1,
                detection.x2,
                detection.y2,
                boxPaint
            )

            // Draw label background
            val labelText = "${detection.className} (${(detection.confidence * 100).toInt()}%)"
            val textBounds = Rect()
            textPaint.getTextBounds(labelText, 0, labelText.length, textBounds)
            val bgRect = RectF(
                detection.x1,
                detection.y1 - textBounds.height() - 8,
                detection.x1 + textBounds.width() + 8,
                detection.y1
            )
            canvas.drawRect(bgRect, bgPaint)

            // Draw label text
            canvas.drawText(labelText, detection.x1 + 4, detection.y1 - 4, textPaint)

            // Draw metrics if available
            metrics[index]?.let { metric ->
                val metricsText = "Speed: ${String.format("%.2f", metric.speed)} px/s"
                canvas.drawText(
                    metricsText,
                    detection.x1,
                    detection.y2 + 20,
                    metricsTextPaint
                )
            }
        }

        // Draw overlay info at top
        drawOverlayInfo(canvas)
    }

    private fun drawOverlayInfo(canvas: Canvas) {
        val infoTexts = listOf(
            "FPS: $fps",
            "Objects: ${detections.size}",
            "Brightness: ${String.format("%.1f", brightness)}"
        )

        var yOffset = 40
        infoTexts.forEach { text ->
            val textBounds = Rect()
            metricsTextPaint.getTextBounds(text, 0, text.length, textBounds)
            val bgRect = RectF(
                10f,
                (yOffset - textBounds.height()).toFloat(),
                (20 + textBounds.width()),
                (yOffset + 5).toFloat()
            )
            canvas.drawRect(bgRect, bgPaint)
            canvas.drawText(text, 15f, yOffset.toFloat(), metricsTextPaint)
            yOffset += 35
        }
    }
}
