package com.accessai.android.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.YuvImage
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.accessai.core.util.CameraFrame
import com.accessai.core.util.CameraService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Android CameraX implementation of CameraService.
 *
 * Provides:
 * - Real-time frame streaming for ML analysis
 * - High-resolution image capture
 * - Front/back camera switching
 * - Automatic lifecycle management
 *
 * Usage:
 *   1. Bind to a PreviewView via [bindToPreview]
 *   2. Collect frames from [frameStream] for ML processing
 *   3. Call [captureImage] for high-res snapshots
 */
class AndroidCameraService(
    private val context: Context
) : CameraService {

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var preview: Preview? = null
    private var isBackCamera = true
    private var _isActive = false

    private val analysisExecutor = Executors.newSingleThreadExecutor()

    /**
     * Bind camera to a PreviewView and lifecycle.
     * Must be called from an Activity/Fragment.
     */
    fun bindToPreview(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(previewView, lifecycleOwner)
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner
    ) {
        val provider = cameraProvider ?: return

        // Camera selector
        val cameraSelector = if (isBackCamera) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        // Preview use case
        preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        // Image capture (high-res photos)
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        // Image analysis (real-time ML frames)
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(android.util.Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()

        // Unbind any existing use cases
        provider.unbindAll()

        // Bind all use cases to lifecycle
        provider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture,
            imageAnalysis
        )

        _isActive = true
    }

    /**
     * Stream camera frames as a Flow for ML processing.
     * Each frame is converted to JPEG bytes for the ML pipeline.
     * Frames are emitted at the analysis rate (~30fps, backpressure: keep latest).
     */
    override fun frameStream(): Flow<CameraFrame> = callbackFlow {
        val analyzer = ImageAnalysis.Analyzer { imageProxy ->
            try {
                val frame = imageProxyToCameraFrame(imageProxy)
                if (frame != null) {
                    trySend(frame)
                }
            } finally {
                imageProxy.close()
            }
        }

        imageAnalysis?.setAnalyzer(analysisExecutor, analyzer)

        awaitClose {
            imageAnalysis?.clearAnalyzer()
        }
    }

    /**
     * Convert CameraX ImageProxy to our CameraFrame model.
     */
    private fun imageProxyToCameraFrame(imageProxy: ImageProxy): CameraFrame? {
        return try {
            val yBuffer = imageProxy.planes[0].buffer
            val uBuffer = imageProxy.planes[1].buffer
            val vBuffer = imageProxy.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            // Convert NV21 to JPEG for the ML pipeline
            val yuvImage = YuvImage(
                nv21,
                ImageFormat.NV21,
                imageProxy.width,
                imageProxy.height,
                null
            )

            val outputStream = ByteArrayOutputStream()
            yuvImage.compressToJpeg(
                android.graphics.Rect(0, 0, imageProxy.width, imageProxy.height),
                80,  // JPEG quality (80% = good quality, reasonable size)
                outputStream
            )

            CameraFrame(
                data = outputStream.toByteArray(),
                width = imageProxy.width,
                height = imageProxy.height,
                rotationDegrees = imageProxy.imageInfo.rotationDegrees,
                timestamp = imageProxy.imageInfo.timestamp
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Capture a single high-resolution JPEG image.
     */
    override suspend fun captureImage(): ByteArray = suspendCoroutine { continuation ->
        val capture = imageCapture ?: run {
            continuation.resumeWithException(
                IllegalStateException("Camera not initialized. Call bindToPreview first.")
            )
            return@suspendCoroutine
        }

        capture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        continuation.resume(bytes)
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    continuation.resumeWithException(exception)
                }
            }
        )
    }

    override fun isActive(): Boolean = _isActive

    override suspend fun switchCamera() {
        isBackCamera = !isBackCamera
        // Rebinding requires the preview and lifecycle owner, which
        // should be triggered from the UI layer by calling bindToPreview again
    }

    override fun release() {
        _isActive = false
        cameraProvider?.unbindAll()
        analysisExecutor.shutdown()
    }
}
