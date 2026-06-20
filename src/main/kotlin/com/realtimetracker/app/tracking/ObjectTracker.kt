package com.realtimetracker.app.tracking

import android.graphics.PointF
import timber.log.Timber
import kotlin.math.hypot

data class TrackedObject(
    val id: Int,
    val center: PointF,
    val confidence: Float,
    val label: String
)

data class TrackingMetrics(
    val distance: Float,
    val speed: Float,
    val acceleration: Float,
    val brightness: Float
)

class ObjectTracker {

    private val trackedObjects = mutableMapOf<Int, MutableList<PointF>>()
    private var frameCount = 0
    private val fps = 30 // Assumed 30 FPS

    fun updateTracking(objects: List<TrackedObject>) {
        frameCount++

        objects.forEach { obj ->
            if (!trackedObjects.containsKey(obj.id)) {
                trackedObjects[obj.id] = mutableListOf()
            }
            trackedObjects[obj.id]?.add(obj.center)
        }

        // Keep only last 30 frames of history
        trackedObjects.forEach { (_, positions) ->
            if (positions.size > 30) {
                positions.removeAt(0)
            }
        }
    }

    fun getMetricsForObject(objectId: Int): TrackingMetrics? {
        val positions = trackedObjects[objectId] ?: return null
        if (positions.size < 2) return null

        val currentPos = positions.last()
        val previousPos = positions[positions.size - 2]

        // Calculate distance (pixel-based)
        val distance = hypot(
            (currentPos.x - previousPos.x).toDouble(),
            (currentPos.y - previousPos.y).toDouble()
        ).toFloat()

        // Calculate speed (pixels per second)
        val speed = distance * fps

        // Calculate acceleration (if we have enough history)
        val acceleration = if (positions.size >= 3) {
            val prevDistance = hypot(
                (positions[positions.size - 2].x - positions[positions.size - 3].x).toDouble(),
                (positions[positions.size - 2].y - positions[positions.size - 3].y).toDouble()
            ).toFloat() * fps
            speed - prevDistance
        } else {
            0f
        }

        Timber.d("Object $objectId - Distance: $distance, Speed: $speed, Acceleration: $acceleration")

        return TrackingMetrics(
            distance = distance,
            speed = speed,
            acceleration = acceleration,
            brightness = 0f // Will be calculated from frame analysis
        )
    }

    fun getTrackedObjectsCount(): Int = trackedObjects.size

    fun clearTracking() {
        trackedObjects.clear()
        frameCount = 0
    }
}
