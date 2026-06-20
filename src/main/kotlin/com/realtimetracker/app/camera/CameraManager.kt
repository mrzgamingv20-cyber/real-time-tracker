package com.realtimetracker.app.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(private val context: Context) {

    private lateinit var cameraProvider: ProcessCameraProvider
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun initializeCamera(onReady: () -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            onReady()
        }, ContextCompat.getMainExecutor(context))
    }

    fun getCameraProvider(): ProcessCameraProvider = cameraProvider

    fun getCameraExecutor(): ExecutorService = cameraExecutor

    fun shutdown() {
        cameraExecutor.shutdown()
        Timber.d("CameraManager shutdown")
    }
}
