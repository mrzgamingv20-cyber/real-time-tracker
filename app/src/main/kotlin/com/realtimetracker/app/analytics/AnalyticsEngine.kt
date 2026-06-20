package com.realtimetracker.app.analytics

import com.realtimetracker.app.data.TrackingSession
import timber.log.Timber
import kotlin.math.sqrt

data class ObjectMotionAnalysis(
    val objectId: Int,
    val label: String,
    val frames: Int,
    val totalDistance: Float,
    val avgSpeed: Float,
    val maxSpeed: Float,
    val avgAcceleration: Float,
    val trajectory: List<Pair<Float, Float>>
)

data class FrameAnalysis(
    val frameNumber: Int,
    val objectCount: Int,
    val avgConfidence: Float,
    val avgBrightness: Float,
    val overallMotion: Float
)

class AnalyticsEngine {

    /**
     * Analyze motion of individual objects across session
     */
    fun analyzeObjectMotion(session: TrackingSession): List<ObjectMotionAnalysis> {
        val objectMotionMap = mutableMapOf<Int, MutableList<Pair<Float, Float>>>()
        val objectLabels = mutableMapOf<Int, String>()
        val objectSpeeds = mutableMapOf<Int, MutableList<Float>>()
        val objectAccelerations = mutableMapOf<Int, MutableList<Float>>()

        session.frames.forEach { frame ->
            frame.detectedObjects.forEach { obj ->
                // Store positions
                if (!objectMotionMap.containsKey(obj.id)) {
                    objectMotionMap[obj.id] = mutableListOf()
                    objectLabels[obj.id] = obj.label
                    objectSpeeds[obj.id] = mutableListOf()
                    objectAccelerations[obj.id] = mutableListOf()
                }
                objectMotionMap[obj.id]?.add(Pair(obj.centerX, obj.centerY))
                objectSpeeds[obj.id]?.add(obj.speed)
                objectAccelerations[obj.id]?.add(obj.distance) // Approximate acceleration
            }
        }

        return objectMotionMap.map { (objectId, trajectory) ->
            val speeds = objectSpeeds[objectId] ?: emptyList()
            val distances = trajectory.zipWithNext { p1, p2 ->
                sqrt(((p2.first - p1.first) * (p2.first - p1.first) + (p2.second - p1.second) * (p2.second - p1.second)).toDouble()).toFloat()
            }

            ObjectMotionAnalysis(
                objectId = objectId,
                label = objectLabels[objectId] ?: "Unknown",
                frames = trajectory.size,
                totalDistance = distances.sum(),
                avgSpeed = if (speeds.isNotEmpty()) speeds.average().toFloat() else 0f,
                maxSpeed = speeds.maxOrNull() ?: 0f,
                avgAcceleration = if (distances.size > 1) {
                    val accelerations = distances.zipWithNext { d1, d2 -> d2 - d1 }
                    accelerations.average().toFloat()
                } else 0f,
                trajectory = trajectory
            )
        }
    }

    /**
     * Analyze individual frames
     */
    fun analyzeFrames(session: TrackingSession): List<FrameAnalysis> {
        return session.frames.map { frame ->
            val objectCount = frame.detectedObjects.size
            val avgConfidence = if (objectCount > 0) {
                frame.detectedObjects.map { it.confidence }.average().toFloat()
            } else 0f

            val overallMotion = if (objectCount > 0) {
                frame.detectedObjects.map { it.speed }.average().toFloat()
            } else 0f

            FrameAnalysis(
                frameNumber = frame.frameNumber,
                objectCount = objectCount,
                avgConfidence = avgConfidence,
                avgBrightness = frame.brightness,
                overallMotion = overallMotion
            )
        }
    }

    /**
     * Detect anomalies in tracking data
     */
    fun detectAnomalies(session: TrackingSession): List<AnomalyDetection> {
        val anomalies = mutableListOf<AnomalyDetection>()
        val frameAnalyses = analyzeFrames(session)

        val avgMotion = frameAnalyses.map { it.overallMotion }.average()
        val motionStdDev = calculateStdDev(frameAnalyses.map { it.overallMotion.toDouble() })

        val avgConfidence = frameAnalyses.map { it.avgConfidence }.average()
        val confidenceStdDev = calculateStdDev(frameAnalyses.map { it.avgConfidence.toDouble() })

        frameAnalyses.forEach { analysis ->
            // Detect sudden motion spikes
            if (analysis.overallMotion > avgMotion + (2 * motionStdDev)) {
                anomalies.add(
                    AnomalyDetection(
                        type = "MotionSpike",
                        frameNumber = analysis.frameNumber,
                        severity = "High",
                        description = "Sudden motion spike detected: ${analysis.overallMotion}"
                    )
                )
            }

            // Detect low confidence detections
            if (analysis.avgConfidence < 0.5f) {
                anomalies.add(
                    AnomalyDetection(
                        type = "LowConfidence",
                        frameNumber = analysis.frameNumber,
                        severity = "Medium",
                        description = "Low confidence detections: ${analysis.avgConfidence}"
                    )
                )
            }

            // Detect lighting issues
            if (analysis.avgBrightness < 50f) {
                anomalies.add(
                    AnomalyDetection(
                        type = "LowLight",
                        frameNumber = analysis.frameNumber,
                        severity = "Medium",
                        description = "Low lighting condition: ${analysis.avgBrightness}"
                    )
                )
            }
        }

        return anomalies
    }

    /**
     * Calculate standard deviation
     */
    private fun calculateStdDev(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
}

data class AnomalyDetection(
    val type: String,
    val frameNumber: Int,
    val severity: String,
    val description: String
)
