package com.example.camerobjecttracking.camera

import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Implémentation CameraX de CameraCapture.
 *
 * @param targetResolution Résolution de l'analyse (défaut 640x480)
 * @param useFront Si true, utilise la caméra frontale
 */
class CameraCaptureImpl(
    private var targetResolution: Size = Size(640, 480),
    private var useFront: Boolean = false
) : CameraCapture {

    private var cameraProviderFuture: ProcessCameraProvider? = null
    private var analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    // FPS measurement
    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()
    private var currentFps = 0

    // Callback utilisateur
    private var userOnFrame: ((ImageProxy) -> Unit)? = null

    override fun start(lifecycleOwner: LifecycleOwner, previewView: PreviewView, onFrame: (ImageProxy) -> Unit) {
        userOnFrame = onFrame

        cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
        cameraProviderFuture?.addListener({
            val provider = cameraProviderFuture?.get() ?: return@addListener
            cameraProvider = provider

            // Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // ImageAnalysis
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(targetResolution)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(analysisExecutor) { imageProxy ->
                        processFrame(imageProxy)
                    }
                }

            val cameraSelector = if (useFront) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                Log.d(TAG, "Camera started: $cameraSelector, res=${targetResolution.width}x${targetResolution.height}")
            } catch (exc: Exception) {
                Log.e(TAG, "Failed to bind camera use cases", exc)
            }
        }, ContextCompat.getMainExecutor(previewView.context))
    }

    private fun processFrame(imageProxy: ImageProxy) {
        // Mesure FPS
        frameCount++
        val now = System.currentTimeMillis()
        if (now - lastFpsTime >= 1000) {
            currentFps = frameCount
            frameCount = 0
            lastFpsTime = now
            Log.d(TAG, "FPS: $currentFps")
        }

        // Transmettre au consommateur
        userOnFrame?.invoke(imageProxy)
        // IMPORTANT : le consommateur DOIT appeler imageProxy.close() lui-même
    }

    override fun stop() {
        try {
            cameraProvider?.unbindAll()
            analysisExecutor.shutdown()
            analysisExecutor.awaitTermination(1, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping camera", e)
        } finally {
            cameraProvider = null
            cameraProviderFuture = null
        }
    }

    override fun setTargetResolution(size: Size) {
        targetResolution = size
    }

    override fun useFrontCamera(enable: Boolean) {
        useFront = enable
        // Redémarrage nécessaire pour appliquer
        if (cameraProvider != null) {
            stop()
            // Le caller doit rappeler start() avec les nouveaux params
        }
    }

    override fun getCurrentFps(): Int = currentFps

    companion object {
        private const val TAG = "CameraCaptureImpl"
    }
}
