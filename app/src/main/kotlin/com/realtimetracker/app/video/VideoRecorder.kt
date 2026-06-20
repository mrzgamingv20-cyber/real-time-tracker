package com.realtimetracker.app.video

import android.content.Context
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.core.util.Consumer
import timber.log.Timber
import java.io.File

class VideoRecorder(private val context: Context) {
    private var recording: Recording? = null
    var isRecording = false
        private set

    fun startRecording(outputFile: File, eventListener: Consumer<VideoRecordEvent>) {
        isRecording = true
        Timber.d("Recording started: ${outputFile.path}")
    }

    fun stopRecording() {
        recording?.stop()
        recording = null
        isRecording = false
        Timber.d("Recording stopped")
    }
}
