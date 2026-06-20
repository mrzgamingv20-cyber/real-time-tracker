package com.realtimetracker.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class Detection(
    val boundingBox: RectF,
    val label: String,
    val confidence: Float
)

class YoloDetector(private val context: Context) {
    private var interpreter: Interpreter? = null
    private val inputSize = 416
    private val numClasses = 80

    fun initialize() {
        try {
            val modelFile = context.assets.open("yolov5.tflite")
            val modelBytes = modelFile.readBytes()
            val buffer = ByteBuffer.allocateDirect(modelBytes.size)
            buffer.put(modelBytes)
            buffer.rewind()
            interpreter = Interpreter(buffer)
            Timber.d("YoloDetector initialized")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize YoloDetector")
        }
    }

    fun detect(bitmap: Bitmap): List<Detection> {
        val interpreter = this.interpreter ?: return emptyList()
        return try {
            val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
            val input = bitmapToByteBuffer(resized)
            val output = Array(1) { Array(10647) { FloatArray(numClasses + 5) } }
            interpreter.run(input, output)
            parseOutput(output[0])
        } catch (e: Exception) {
            Timber.e(e, "Detection failed")
            emptyList()
        }
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
            buffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)
            buffer.putFloat((pixel and 0xFF) / 255.0f)
        }
        return buffer
    }

    private fun parseOutput(output: Array<FloatArray>): List<Detection> {
        val detections = mutableListOf<Detection>()
        for (row in output) {
            val confidence = row[4]
            if (confidence > 0.5f) {
                val x = row[0]
                val y = row[1]
                val w = row[2]
                val h = row[3]
                val rect = RectF(x - w/2, y - h/2, x + w/2, y + h/2)
                detections.add(Detection(rect, "object", confidence))
            }
        }
        return detections
    }

    fun close() {
        interpreter?.close()
    }
}
