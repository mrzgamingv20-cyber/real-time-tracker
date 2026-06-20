package com.realtimetracker.app.export

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.realtimetracker.app.data.FrameData
import com.realtimetracker.app.data.TrackingSession
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DataExporter(private val context: Context) {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val exportDir = File(context.getExternalFilesDir(null), "exports")

    init {
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
    }

    /**
     * Export tracking session to JSON format
     */
    fun exportSessionToJson(session: TrackingSession): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "session_${session.sessionId}_${timestamp}.json"
            val file = File(exportDir, fileName)

            val json = gson.toJson(session)
            file.writeText(json)

            Timber.d("Session exported to JSON: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Timber.e(e, "Failed to export session to JSON")
            null
        }
    }

    /**
     * Export tracking session to CSV format
     */
    fun exportSessionToCsv(session: TrackingSession): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "session_${session.sessionId}_${timestamp}.csv"
            val file = File(exportDir, fileName)

            val csvContent = StringBuilder()
            csvContent.append("Frame,Timestamp,ObjectID,Label,Confidence,CenterX,CenterY,Distance(px),Speed(px/s),Brightness\n")

            session.frames.forEach { frame ->
                frame.detectedObjects.forEach { obj ->
                    csvContent.append(
                        "${frame.frameNumber},${frame.timestamp},${obj.id},${obj.label}," +
                                "${obj.confidence},${obj.centerX},${obj.centerY}," +
                                "${obj.distance},${obj.speed},${frame.brightness}\n"
                    )
                }
            }

            file.writeText(csvContent.toString())

            Timber.d("Session exported to CSV: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Timber.e(e, "Failed to export session to CSV")
            null
        }
    }

    /**
     * Generate statistics summary
     */
    fun generateStatistics(session: TrackingSession): SessionStatistics {
        val totalFrames = session.frames.size
        val totalObjects = session.frames.sumOf { it.detectedObjects.size }
        val uniqueObjects = session.frames.flatMap { it.detectedObjects.map { obj -> obj.id } }.toSet().size

        val speeds = session.frames.flatMap { frame -> frame.detectedObjects.map { it.speed } }
        val distances = session.frames.flatMap { frame -> frame.detectedObjects.map { it.distance } }
        val brightness = session.frames.map { it.brightness }
        val confidences = session.frames.flatMap { frame -> frame.detectedObjects.map { it.confidence } }

        return SessionStatistics(
            sessionId = session.sessionId,
            totalFrames = totalFrames,
            totalDetections = totalObjects,
            uniqueObjects = uniqueObjects,
            avgSpeed = if (speeds.isNotEmpty()) speeds.average() else 0.0,
            maxSpeed = speeds.maxOrNull() ?: 0f,
            minSpeed = speeds.minOrNull() ?: 0f,
            totalDistance = distances.sum(),
            avgDistance = if (distances.isNotEmpty()) distances.average() else 0.0,
            avgBrightness = if (brightness.isNotEmpty()) brightness.average() else 0.0,
            avgConfidence = if (confidences.isNotEmpty()) confidences.average() else 0.0,
            duration = (session.frames.lastOrNull()?.timestamp ?: 0) - (session.frames.firstOrNull()?.timestamp ?: 0)
        )
    }

    /**
     * Export statistics to JSON
     */
    fun exportStatisticsToJson(statistics: SessionStatistics): File? {
        return try {
            val fileName = "stats_${statistics.sessionId}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.json"
            val file = File(exportDir, fileName)

            val json = gson.toJson(statistics)
            file.writeText(json)

            Timber.d("Statistics exported: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Timber.e(e, "Failed to export statistics")
            null
        }
    }

    /**
     * Get all exported files
     */
    fun getExportedFiles(): List<File> {
        return try {
            exportDir.listFiles()?.toList() ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get exported files")
            emptyList()
        }
    }

    /**
     * Delete exported file
     */
    fun deleteExportedFile(fileName: String): Boolean {
        return try {
            val file = File(exportDir, fileName)
            val deleted = file.delete()
            if (deleted) Timber.d("File deleted: $fileName")
            deleted
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete file")
            false
        }
    }
}

data class SessionStatistics(
    val sessionId: String,
    val totalFrames: Int,
    val totalDetections: Int,
    val uniqueObjects: Int,
    val avgSpeed: Double,
    val maxSpeed: Float,
    val minSpeed: Float,
    val totalDistance: Float,
    val avgDistance: Double,
    val avgBrightness: Double,
    val avgConfidence: Double,
    val duration: Long
)
