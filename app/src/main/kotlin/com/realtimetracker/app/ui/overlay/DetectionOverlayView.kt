package com.realtimetracker.app.ui.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.realtimetracker.app.ml.Detection

class DetectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
    }

    private var detections: List<Detection> = emptyList()

    fun updateDetections(detections: List<Detection>) {
        this.detections = detections
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (detection in detections) {
            val box = RectF(
                detection.boundingBox.left * width,
                detection.boundingBox.top * height,
                detection.boundingBox.right * width,
                detection.boundingBox.bottom * height
            )
            canvas.drawRect(box, boxPaint)
            canvas.drawText(
                "${detection.label} ${"%.0f".format(detection.confidence * 100)}%",
                box.left,
                box.top - 10f,
                textPaint
            )
        }
    }
}
