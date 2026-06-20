package com.realtimetracker.app.video

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.view.Surface
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class VideoRecorder(private val context: Context) {

    companion object {
        private const val VIDEO_WIDTH = 1280
        private const val VIDEO_HEIGHT = 720
        private const val VIDEO_BITRATE = 10_000_000 // 10 Mbps
        private const val FRAME_RATE = 30
        private const val I_FRAME_INTERVAL = 1
    }

    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var recordingSurface: Surface? = null
    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var isRecording = false
    private val executor = Executors.newSingleThreadExecutor()
    private var outputFile: File? = null

    fun startRecording(onReady: (Surface) -> Unit): File {
        try {
            // Create output file
            val storageDir = File(context.getExternalFilesDir(null), "recordings")
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            outputFile = File(storageDir, "tracking_${timestamp}.mp4")

            // Setup MediaCodec for video encoding
            val videoFormat = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC,
                VIDEO_WIDTH,
                VIDEO_HEIGHT
            ).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BITRATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodec.CodecCapabilities.COLOR_FormatSurface
                )
            }

            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            mediaCodec?.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            recordingSurface = mediaCodec?.createInputSurface()
            mediaCodec?.start()

            // Setup MediaMuxer
            mediaMuxer = MediaMuxer(outputFile!!.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            isRecording = true

            // Start encoding thread
            startEncodingThread()

            onReady(recordingSurface!!)
            Timber.d("Recording started: ${outputFile?.absolutePath}")

            return outputFile!!
        } catch (e: Exception) {
            Timber.e(e, "Failed to start recording")
            throw e
        }
    }

    private fun startEncodingThread() {
        executor.execute {
            val bufferInfo = MediaCodec.BufferInfo()
            val timeout = 10000L // 10ms timeout

            while (isRecording) {
                try {
                    val outputBufferIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, timeout) ?: -1

                    if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        val newFormat = mediaCodec?.outputFormat
                        if (newFormat != null && videoTrackIndex == -1) {
                            videoTrackIndex = mediaMuxer?.addTrack(newFormat) ?: -1
                            if (videoTrackIndex >= 0 && audioTrackIndex >= 0) {
                                mediaMuxer?.start()
                            }
                        }
                    } else if (outputBufferIndex >= 0) {
                        if (videoTrackIndex >= 0) {
                            val outputBuffer = mediaCodec?.getOutputBuffer(outputBufferIndex)
                            if (outputBuffer != null) {
                                mediaMuxer?.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                            }
                        }
                        mediaCodec?.releaseOutputBuffer(outputBufferIndex, false)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Encoding error")
                    Thread.sleep(100)
                }
            }
        }
    }

    fun stopRecording(): File? {
        return try {
            isRecording = false
            Thread.sleep(500) // Wait for last frames

            // Signal end of stream
            mediaCodec?.signalEndOfInputStream()
            Thread.sleep(500)

            mediaMuxer?.stop()
            mediaMuxer?.release()
            mediaCodec?.stop()
            mediaCodec?.release()
            recordingSurface?.release()

            Timber.d("Recording stopped: ${outputFile?.absolutePath}")
            outputFile
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop recording")
            null
        }
    }

    fun getOutputFile(): File? = outputFile

    fun shutdown() {
        isRecording = false
        executor.shutdown()
        Timber.d("VideoRecorder shutdown")
    }
}
