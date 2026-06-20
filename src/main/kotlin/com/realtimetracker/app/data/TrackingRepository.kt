package com.realtimetracker.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class TrackingSession(
    val sessionId: String,
    val timestamp: Long,
    val frames: List<FrameData>
)

data class FrameData(
    val frameNumber: Int,
    val timestamp: Long,
    val detectedObjects: List<DetectedObjectData>,
    val brightness: Float
)

data class DetectedObjectData(
    val id: Int,
    val label: String,
    val confidence: Float,
    val centerX: Float,
    val centerY: Float,
    val distance: Float,
    val speed: Float
)

class TrackingRepository(private val context: Context) {

    private val gson = Gson()
    private val storageDir = File(context.getExternalFilesDir(null), "tracking_sessions")

    init {
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
    }

    fun saveSession(session: TrackingSession) {
        try {
            val fileName = "session_${session.sessionId}.json"
            val file = File(storageDir, fileName)
            val json = gson.toJson(session)
            file.writeText(json)
            Timber.d("Session saved: $fileName")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save session")
        }
    }

    fun loadSession(sessionId: String): TrackingSession? {
        return try {
            val fileName = "session_$sessionId.json"
            val file = File(storageDir, fileName)
            if (!file.exists()) return null

            val json = file.readText()
            val type = object : TypeToken<TrackingSession>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load session")
            null
        }
    }

    fun getSessions(): List<TrackingSession> {
        return try {
            storageDir.listFiles()?.mapNotNull { file ->
                if (file.extension == "json") {
                    val json = file.readText()
                    val type = object : TypeToken<TrackingSession>() {}.type
                    gson.fromJson<TrackingSession>(json, type)
                } else null
            } ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get sessions")
            emptyList()
        }
    }

    fun deleteSession(sessionId: String) {
        try {
            val fileName = "session_$sessionId.json"
            val file = File(storageDir, fileName)
            if (file.exists()) {
                file.delete()
                Timber.d("Session deleted: $fileName")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete session")
        }
    }
}
