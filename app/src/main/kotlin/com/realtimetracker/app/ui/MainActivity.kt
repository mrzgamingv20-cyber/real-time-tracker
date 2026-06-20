package com.realtimetracker.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.realtimetracker.app.camera.CameraManager
import com.realtimetracker.app.databinding.ActivityMainBinding
import com.realtimetracker.app.ml.YoloDetection
import com.realtimetracker.app.ml.YoloDetector
import com.realtimetracker.app.tracking.ObjectTracker
import com.realtimetracker.app.tracking.TrackedObject
import com.realtimetracker.app.video.VideoRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraManager: CameraManager
    private lateinit var yoloDetector: YoloDetector
    private lateinit var objectTracker: ObjectTracker
    private lateinit var videoRecorder: VideoRecorder

    private val PERMISSION_REQUESTS = 1
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private var isRecording = false
    private var frameCount = 0
    private var lastFpsTime = 0L
    private var currentFps = 0
    private var imageAnalysis: ImageAnalysis? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Timber.plant(Timber.DebugTree())
        Timber.d("MainActivity created")

        // Initialize components
        cameraManager = CameraManager(this)
        yoloDetector = YoloDetector(this)
        objectTracker = ObjectTracker()
        videoRecorder = VideoRecorder(this)

        // Setup button listeners
        binding.btnRecord.setOnClickListener { toggleRecording() }
        binding.btnCapture.setOnClickListener { captureFrame() }
        binding.btnExport.setOnClickListener { exportData() }

        if (!allPermissionsGranted()) {
            requestPermissions()
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        cameraManager.initializeCamera {
            bindPreview()
        }
    }

    private fun bindPreview() {
        val cameraProvider = cameraManager.getCameraProvider()
        val preview = Preview.Builder()
            .setTargetRotation(binding.previewView.display.rotation)
            .build()
            .also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

        // Setup ImageAnalysis for object detection
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(android.util.Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraManager.getCameraExecutor()) { imageProxy ->
                    processFrame(imageProxy)
                }
            }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
            Timber.d("Camera bound successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to bind camera")
        }
    }

    private fun processFrame(imageProxy: ImageProxy) {
        try {
            // Convert ImageProxy to Bitmap
            val bitmap = imageProxy.toBitmap()

            // Run YOLO detection
            val detections = yoloDetector.detectObjects(bitmap)

            // Convert detections to tracked objects
            val trackedObjects = detections.mapIndexed { index, detection ->
                TrackedObject(
                    id = index,
                    center = android.graphics.PointF(detection.centerX, detection.centerY),
                    confidence = detection.confidence,
                    label = detection.className
                )
            }

            // Update tracking
            objectTracker.updateTracking(trackedObjects)

            // Get metrics
            val metrics = trackedObjects.associate { obj ->
                obj.id to (objectTracker.getMetricsForObject(obj.id)
                    ?: com.realtimetracker.app.tracking.TrackingMetrics(
                        distance = 0f,
                        speed = 0f,
                        acceleration = 0f,
                        brightness = 0f
                    ))
            }

            // Analyze brightness
            val brightness = yoloDetector.analyzeBrightness(bitmap)

            // Update UI
            updateUI(detections, metrics, brightness)

            // Update frame count for FPS
            frameCount++
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastFpsTime >= 1000) {
                currentFps = frameCount
                frameCount = 0
                lastFpsTime = currentTime
            }

            bitmap.recycle()
        } catch (e: Exception) {
            Timber.e(e, "Frame processing error")
        } finally {
            imageProxy.close()
        }
    }

    private fun updateUI(
        detections: List<YoloDetection>,
        metrics: Map<Int, com.realtimetracker.app.tracking.TrackingMetrics>,
        brightness: Float
    ) {
        runOnUiThread {
            binding.tvObjectCount.text = "Objects: ${detections.size}"
            binding.tvBrightness.text = "Brightness: ${String.format("%.1f", brightness)}"

            val avgSpeed = metrics.values.map { it.speed }.average().takeIf { !it.isNaN() } ?: 0f
            binding.tvSpeed.text = "Avg Speed: ${String.format("%.2f", avgSpeed)} px/s"

            // Update overlay
            binding.detectionOverlay.updateDetections(detections)
            binding.detectionOverlay.updateMetrics(metrics)
            binding.detectionOverlay.updateBrightness(brightness)
            binding.detectionOverlay.updateFPS(currentFps)
        }
    }

    private fun toggleRecording() {
        isRecording = if (isRecording) {
            stopRecording()
            false
        } else {
            startRecording()
            true
        }
        updateRecordingButton()
    }

    private fun startRecording() {
        try {
            videoRecorder.startRecording { surface ->
                // Attach recording surface to camera
                Timber.d("Recording surface ready")
            }
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Timber.e(e, "Failed to start recording")
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        val file = videoRecorder.stopRecording()
        Toast.makeText(this, "Recording saved: ${file?.name}", Toast.LENGTH_SHORT).show()
    }

    private fun updateRecordingButton() {
        binding.btnRecord.text = if (isRecording) "Stop Recording" else "Start Recording"
    }

    private fun captureFrame() {
        Timber.d("Frame captured")
        Toast.makeText(this, "Frame captured", Toast.LENGTH_SHORT).show()
    }

    private fun exportData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Export data to JSON/CSV
                Timber.d("Exporting data...")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Data exported", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Timber.e(e, "Export failed")
            }
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                PERMISSION_REQUESTS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUESTS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun ImageProxy.toBitmap(): Bitmap {
        val image = this.image ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val planes = image.planes
        val buffer = planes[0].buffer
        buffer.rewind()
        val pixelStride = planes[0].pixelStride
        val rowPadding = planes[0].rowPadding
        val rowStride = planes[0].rowStride
        val w = image.width
        val h = image.height

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixelArray = IntArray(w * h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val index = y * w + x
                pixelArray[index] = buffer.get(y * rowStride + x * pixelStride).toInt()
            }
        }

        return bitmap
    }

    override fun onDestroy() {
        super.onDestroy()
        yoloDetector.shutdown()
        cameraManager.shutdown()
        videoRecorder.shutdown()
    }
}
