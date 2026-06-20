package com.realtimetracker.app.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.realtimetracker.app.R
import com.realtimetracker.app.analytics.AnalyticsEngine
import com.realtimetracker.app.data.TrackingRepository
import com.realtimetracker.app.databinding.ActivityAnalyticsBinding
import com.realtimetracker.app.export.DataExporter
import timber.log.Timber

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalyticsBinding
    private lateinit var repository: TrackingRepository
    private lateinit var exporter: DataExporter
    private lateinit var analyticsEngine: AnalyticsEngine
    private var currentSessionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = TrackingRepository(this)
        exporter = DataExporter(this)
        analyticsEngine = AnalyticsEngine()

        currentSessionId = intent.getStringExtra("session_id")

        loadAnalytics()
        setupButtons()
    }

    private fun loadAnalytics() {
        currentSessionId?.let { sessionId ->
            val session = repository.loadSession(sessionId)
            if (session != null) {
                displayStatistics(session)
                displayMotionAnalysis(session)
                displayFrameAnalysis(session)
                displayAnomalies(session)
            } else {
                Timber.e("Session not found: $sessionId")
            }
        }
    }

    private fun displayStatistics(session: com.realtimetracker.app.data.TrackingSession) {
        val stats = exporter.generateStatistics(session)

        binding.tvTotalFrames.text = "Total Frames: ${stats.totalFrames}"
        binding.tvTotalDetections.text = "Total Detections: ${stats.totalDetections}"
        binding.tvUniqueObjects.text = "Unique Objects: ${stats.uniqueObjects}"
        binding.tvAvgSpeed.text = String.format("Avg Speed: %.2f px/s", stats.avgSpeed)
        binding.tvMaxSpeed.text = String.format("Max Speed: %.2f px/s", stats.maxSpeed)
        binding.tvTotalDistance.text = String.format("Total Distance: %.2f px", stats.totalDistance)
        binding.tvAvgBrightness.text = String.format("Avg Brightness: %.2f", stats.avgBrightness)
        binding.tvAvgConfidence.text = String.format("Avg Confidence: %.2f%%", stats.avgConfidence * 100)
        binding.tvDuration.text = String.format("Duration: %d ms", stats.duration)
    }

    private fun displayMotionAnalysis(session: com.realtimetracker.app.data.TrackingSession) {
        val motionAnalyses = analyticsEngine.analyzeObjectMotion(session)
        binding.motionGraphView.updateMotionData(motionAnalyses)

        // Display motion details
        var detailText = "Motion Analysis:\n\n"
        motionAnalyses.forEach { analysis ->
            detailText += "${analysis.label} (ID: ${analysis.objectId}):\n" +
                    "  Frames: ${analysis.frames}\n" +
                    "  Total Distance: ${String.format("%.2f", analysis.totalDistance)} px\n" +
                    "  Avg Speed: ${String.format("%.2f", analysis.avgSpeed)} px/s\n" +
                    "  Max Speed: ${String.format("%.2f", analysis.maxSpeed)} px/s\n" +
                    "  Avg Acceleration: ${String.format("%.2f", analysis.avgAcceleration)} px/s²\n\n"
        }
        binding.tvMotionDetails.text = detailText
    }

    private fun displayFrameAnalysis(session: com.realtimetracker.app.data.TrackingSession) {
        val frameAnalyses = analyticsEngine.analyzeFrames(session)

        val maxObjects = frameAnalyses.maxOf { it.objectCount }
        val avgConfidence = frameAnalyses.map { it.avgConfidence }.average()

        binding.tvFrameStats.text = "Frame Statistics:\n" +
                "Max Objects per Frame: $maxObjects\n" +
                "Avg Confidence: ${String.format("%.2f%%", avgConfidence * 100)}\n" +
                "Total Frames Analyzed: ${frameAnalyses.size}"
    }

    private fun displayAnomalies(session: com.realtimetracker.app.data.TrackingSession) {
        val anomalies = analyticsEngine.detectAnomalies(session)

        if (anomalies.isEmpty()) {
            binding.tvAnomalies.text = "No anomalies detected ✓"
        } else {
            var anomalyText = "Anomalies Detected (${anomalies.size}):\n\n"
            anomalies.groupBy { it.frameNumber }.forEach { (frame, frameAnomalies) ->
                anomalyText += "Frame $frame:\n"
                frameAnomalies.forEach { anomaly ->
                    anomalyText += "  [${anomaly.severity}] ${anomaly.type}: ${anomaly.description}\n"
                }
                anomalyText += "\n"
            }
            binding.tvAnomalies.text = anomalyText
        }
    }

    private fun setupButtons() {
        binding.btnExportJson.setOnClickListener {
            currentSessionId?.let { sessionId ->
                val session = repository.loadSession(sessionId)
                if (session != null) {
                    val file = exporter.exportSessionToJson(session)
                    if (file != null) {
                        showMessage("Exported to: ${file.name}")
                    }
                }
            }
        }

        binding.btnExportCsv.setOnClickListener {
            currentSessionId?.let { sessionId ->
                val session = repository.loadSession(sessionId)
                if (session != null) {
                    val file = exporter.exportSessionToCsv(session)
                    if (file != null) {
                        showMessage("Exported to: ${file.name}")
                    }
                }
            }
        }

        binding.btnExportStats.setOnClickListener {
            currentSessionId?.let { sessionId ->
                val session = repository.loadSession(sessionId)
                if (session != null) {
                    val stats = exporter.generateStatistics(session)
                    val file = exporter.exportStatisticsToJson(stats)
                    if (file != null) {
                        showMessage("Statistics exported to: ${file.name}")
                    }
                }
            }
        }
    }

    private fun showMessage(message: String) {
        Timber.d(message)
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
