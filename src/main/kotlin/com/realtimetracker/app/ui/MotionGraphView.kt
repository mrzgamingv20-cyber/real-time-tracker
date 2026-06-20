package com.realtimetracker.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.realtimetracker.app.analytics.ObjectMotionAnalysis
import kotlin.math.max
import kotlin.math.min

class MotionGraphView(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var motionData: List<ObjectMotionAnalysis> = emptyList()
    private val graphPaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 2f
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 12f
        isAntiAlias = true
    }
    private val axisPaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 1f
    }

    private val colors = listOf(
        Color.RED, Color.GREEN, Color.BLUE,
        Color.CYAN, Color.MAGENTA, Color.YELLOW
    )

    fun updateMotionData(data: List<ObjectMotionAnalysis>) {
        motionData = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (motionData.isEmpty()) {
            canvas.drawText("No motion data", 50f, 50f, textPaint)
            return
        }

        val padding = 60f
        val graphWidth = width - 2 * padding
        val graphHeight = height - 2 * padding

        // Draw axes
        canvas.drawLine(padding, height - padding, width - padding, height - padding, axisPaint)
        canvas.drawLine(padding, padding, padding, height - padding, axisPaint)

        // Draw grid
        drawGrid(canvas, padding, graphHeight)

        // Draw motion trajectories
        motionData.forEachIndexed { index, analysis ->
            val color = colors[index % colors.size]
            drawTrajectory(canvas, analysis, padding, graphWidth, graphHeight, color)
        }

        // Draw legend
        drawLegend(canvas, padding)
    }

    private fun drawTrajectory(
        canvas: Canvas,
        analysis: ObjectMotionAnalysis,
        padding: Float,
        graphWidth: Float,
        graphHeight: Float,
        color: Int
    ) {
        if (analysis.trajectory.isEmpty()) return

        val maxX = analysis.trajectory.maxOf { it.first }
        val maxY = analysis.trajectory.maxOf { it.second }

        graphPaint.color = color
        graphPaint.style = Paint.Style.STROKE

        for (i in 0 until analysis.trajectory.size - 1) {
            val p1 = analysis.trajectory[i]
            val p2 = analysis.trajectory[i + 1]

            val x1 = padding + (p1.first / maxX) * graphWidth
            val y1 = height - padding - (p1.second / maxY) * graphHeight
            val x2 = padding + (p2.first / maxX) * graphWidth
            val y2 = height - padding - (p2.second / maxY) * graphHeight

            canvas.drawLine(x1, y1, x2, y2, graphPaint)
        }
    }

    private fun drawGrid(canvas: Canvas, padding: Float, graphHeight: Float) {
        val gridCount = 5
        for (i in 0..gridCount) {
            val y = padding + (graphHeight / gridCount) * i
            canvas.drawLine(padding - 10, y, width - padding, y, axisPaint)
        }
    }

    private fun drawLegend(canvas: Canvas, padding: Float) {
        var yOffset = padding + 20
        motionData.forEachIndexed { index, analysis ->
            val color = colors[index % colors.size]
            graphPaint.color = color
            canvas.drawCircle(padding + 20, yOffset, 5f, graphPaint)
            textPaint.color = Color.WHITE
            canvas.drawText(
                "${analysis.label} (ID: ${analysis.objectId})",
                padding + 35,
                yOffset + 5,
                textPaint
            )
            yOffset += 20
        }
    }
}
