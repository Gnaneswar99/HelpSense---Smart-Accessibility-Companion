package com.accessai.android.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.accessai.core.ml.ObjectDetectionEngine
import com.accessai.core.model.BoundingBox
import com.accessai.core.model.DetectedObject
import com.accessai.core.model.InferenceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * TensorFlow Lite object detection engine.
 *
 * Uses a MobileNet SSD v2 model for real-time object detection.
 * Detects objects with bounding boxes and confidence scores.
 *
 * Input: 300x300 RGB image (uint8)
 * Output: Bounding boxes, class labels, confidence scores, detection count
 *
 * This engine is used by both the Vision module (describe what's in front)
 * and the Navigation module (detect obstacles).
 */
class TFLiteObjectDetectionEngine(
    private val context: Context,
    private val modelManager: TFLiteModelManager
) : ObjectDetectionEngine {

    companion object {
        const val INPUT_SIZE = 300
        const val MAX_DETECTIONS = 10
        const val PIXEL_SIZE = 3  // RGB
        const val NUM_BYTES_PER_CHANNEL = 1  // uint8
    }

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    /**
     * Load the detection model and labels.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        interpreter = modelManager.loadModel(TFLiteModelManager.MODEL_OBJECT_DETECTION)
        labels = modelManager.loadLabels(TFLiteModelManager.LABELS_OBJECT_DETECTION)
        interpreter != null
    }

    override suspend fun detect(
        imageBytes: ByteArray,
        maxResults: Int,
        confidenceThreshold: Float
    ): InferenceResult<List<DetectedObject>> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        val interp = interpreter
        if (interp == null) {
            return@withContext InferenceResult.Error(
                "Object detection model not loaded. Place 'detect.tflite' in assets/models/"
            )
        }

        try {
            // Decode JPEG to Bitmap
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ?: return@withContext InferenceResult.Error("Failed to decode image")

            // Resize to model input size
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)

            // Convert to ByteBuffer
            val inputBuffer = bitmapToByteBuffer(resizedBitmap)

            // Prepare output arrays
            // MobileNet SSD outputs:
            // [0] Bounding boxes: [1, MAX_DETECTIONS, 4] (top, left, bottom, right)
            // [1] Class labels:   [1, MAX_DETECTIONS]
            // [2] Confidence:     [1, MAX_DETECTIONS]
            // [3] Detection count: [1]
            val outputBoxes = Array(1) { Array(MAX_DETECTIONS) { FloatArray(4) } }
            val outputClasses = Array(1) { FloatArray(MAX_DETECTIONS) }
            val outputScores = Array(1) { FloatArray(MAX_DETECTIONS) }
            val outputCount = FloatArray(1)

            val outputMap = mapOf(
                0 to outputBoxes,
                1 to outputClasses,
                2 to outputScores,
                3 to outputCount
            )

            // Run inference
            interp.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputMap)

            // Parse results
            val count = outputCount[0].toInt().coerceAtMost(maxResults)
            val detections = mutableListOf<DetectedObject>()

            for (i in 0 until count) {
                val score = outputScores[0][i]
                if (score >= confidenceThreshold) {
                    val classIndex = outputClasses[0][i].toInt()
                    val label = if (classIndex < labels.size) labels[classIndex] else "Object $classIndex"

                    val box = outputBoxes[0][i]
                    detections.add(
                        DetectedObject(
                            label = label,
                            confidence = score,
                            boundingBox = BoundingBox(
                                top = box[0],
                                left = box[1],
                                bottom = box[2],
                                right = box[3]
                            )
                        )
                    )
                }
            }

            // Clean up bitmaps
            if (resizedBitmap != bitmap) resizedBitmap.recycle()
            bitmap.recycle()

            val inferenceTime = System.currentTimeMillis() - startTime

            InferenceResult.Success(
                data = detections.sortedByDescending { it.confidence },
                confidenceScore = detections.firstOrNull()?.confidence ?: 0f,
                inferenceTimeMs = inferenceTime
            )
        } catch (e: Exception) {
            InferenceResult.Error("Detection failed: ${e.message}", e)
        }
    }

    /**
     * Convert Bitmap to ByteBuffer for model input.
     */
    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(
            1 * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE * NUM_BYTES_PER_CHANNEL
        )
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (pixel in pixels) {
            // Extract RGB channels (uint8 range 0-255)
            byteBuffer.put(((pixel shr 16) and 0xFF).toByte())  // R
            byteBuffer.put(((pixel shr 8) and 0xFF).toByte())   // G
            byteBuffer.put((pixel and 0xFF).toByte())            // B
        }

        byteBuffer.rewind()
        return byteBuffer
    }
}
