package com.accessai.android.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.accessai.core.ml.ImageCaptionEngine
import com.accessai.core.model.CaptionResult
import com.accessai.core.model.DetectedObject
import com.accessai.core.model.InferenceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * TFLite-based image labeling and scene description engine.
 *
 * Strategy: Uses a MobileNet image classification model to identify
 * the top scene labels, then combines them with object detection results
 * to generate a natural language caption.
 *
 * This is a practical approach that works well on-device without needing
 * a full encoder-decoder captioning model (which would be ~200MB+).
 *
 * Input: 224x224 RGB image (float32, normalized 0-1)
 * Output: Probability vector over 1001 ImageNet classes
 *
 * The caption is constructed by:
 * 1. Running image classification for scene/context labels
 * 2. Running object detection for specific objects
 * 3. Combining both into a descriptive sentence
 */
class TFLiteImageCaptionEngine(
    private val context: Context,
    private val modelManager: TFLiteModelManager,
    private val objectDetectionEngine: TFLiteObjectDetectionEngine
) : ImageCaptionEngine {

    companion object {
        const val INPUT_SIZE = 224
        const val PIXEL_SIZE = 3
        const val NUM_CLASSES = 1001  // ImageNet classes
        const val FLOAT_SIZE = 4
        const val IMAGE_MEAN = 127.5f
        const val IMAGE_STD = 127.5f
        const val TOP_K = 5
    }

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    /**
     * Load the image classification model and labels.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        interpreter = modelManager.loadModel(TFLiteModelManager.MODEL_IMAGE_LABELING)
        labels = modelManager.loadLabels(TFLiteModelManager.LABELS_IMAGE)

        // Also ensure object detection is initialized
        objectDetectionEngine.initialize()

        interpreter != null
    }

    override suspend fun caption(imageBytes: ByteArray): InferenceResult<CaptionResult> =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            try {
                // Run both classification and detection in parallel
                val classificationResult = classifyImage(imageBytes)
                val detectionResult = objectDetectionEngine.detect(
                    imageBytes, maxResults = 5, confidenceThreshold = 0.4f
                )

                // Build caption from results
                val topLabels = classificationResult.first  // top scene labels
                val topConfidence = classificationResult.second
                val detectedObjects = when (detectionResult) {
                    is InferenceResult.Success -> detectionResult.data
                    else -> emptyList()
                }

                val caption = buildCaption(topLabels, detectedObjects)
                val inferenceTime = System.currentTimeMillis() - startTime

                InferenceResult.Success(
                    data = CaptionResult(
                        caption = caption,
                        detectedObjects = detectedObjects,
                        sceneType = topLabels.firstOrNull() ?: "unknown",
                        confidence = topConfidence
                    ),
                    confidenceScore = topConfidence,
                    inferenceTimeMs = inferenceTime
                )
            } catch (e: Exception) {
                InferenceResult.Error("Captioning failed: ${e.message}", e)
            }
        }

    /**
     * Run image classification and return top-K labels with confidence.
     */
    private fun classifyImage(imageBytes: ByteArray): Pair<List<String>, Float> {
        val interp = interpreter
            ?: return Pair(listOf("scene"), 0.5f)

        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return Pair(listOf("image"), 0.5f)

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val inputBuffer = bitmapToFloatBuffer(resizedBitmap)

        // Output: probability for each class
        val output = Array(1) { FloatArray(NUM_CLASSES) }
        interp.run(inputBuffer, output)

        // Get top-K labels
        val probabilities = output[0]
        val topIndices = probabilities.indices
            .sortedByDescending { probabilities[it] }
            .take(TOP_K)

        val topLabels = topIndices.mapNotNull { idx ->
            if (idx < labels.size && probabilities[idx] > 0.1f) {
                labels[idx].lowercase().replace("_", " ")
            } else null
        }

        val topConfidence = probabilities[topIndices.first()]

        // Cleanup
        if (resizedBitmap != bitmap) resizedBitmap.recycle()
        bitmap.recycle()

        return Pair(
            topLabels.ifEmpty { listOf("scene") },
            topConfidence
        )
    }

    /**
     * Build a natural language caption from classification labels and detected objects.
     *
     * Examples:
     * - "I see a living room with a couch, a table, and a lamp"
     * - "This looks like an outdoor park. I can see 2 people and a dog"
     * - "I see a kitchen with a refrigerator and a microwave"
     */
    private fun buildCaption(
        sceneLabels: List<String>,
        detectedObjects: List<DetectedObject>
    ): String {
        val sb = StringBuilder()

        // Scene description
        val mainScene = sceneLabels.firstOrNull() ?: "scene"
        if (sceneLabels.size > 1) {
            sb.append("This looks like a $mainScene.")
        } else {
            sb.append("I see a $mainScene.")
        }

        // Object descriptions
        if (detectedObjects.isNotEmpty()) {
            sb.append(" ")

            // Group by label and count
            val objectCounts = detectedObjects
                .groupBy { it.label.lowercase() }
                .map { (label, items) -> label to items.size }
                .sortedByDescending { it.second }

            when (objectCounts.size) {
                1 -> {
                    val (label, count) = objectCounts[0]
                    if (count > 1) {
                        sb.append("I can see $count ${label}s.")
                    } else {
                        sb.append("I can see a $label.")
                    }
                }
                2 -> {
                    val first = formatObjectCount(objectCounts[0])
                    val second = formatObjectCount(objectCounts[1])
                    sb.append("I can see $first and $second.")
                }
                else -> {
                    sb.append("I can see ")
                    objectCounts.forEachIndexed { index, pair ->
                        val desc = formatObjectCount(pair)
                        when {
                            index == objectCounts.size - 1 -> sb.append("and $desc.")
                            index < objectCounts.size - 1 -> sb.append("$desc, ")
                        }
                    }
                }
            }

            // Spatial hints for the most prominent object
            val topObject = detectedObjects.maxByOrNull { it.confidence }
            topObject?.boundingBox?.let { box ->
                val position = describePosition(box.left, box.top, box.right, box.bottom)
                if (position.isNotEmpty()) {
                    sb.append(" The ${topObject.label} is $position.")
                }
            }
        }

        return sb.toString()
    }

    /**
     * Format object count for caption (e.g., "a dog", "2 chairs").
     */
    private fun formatObjectCount(pair: Pair<String, Int>): String {
        val (label, count) = pair
        return if (count > 1) "$count ${label}s" else "a $label"
    }

    /**
     * Describe the position of an object based on its bounding box.
     */
    private fun describePosition(left: Float, top: Float, right: Float, bottom: Float): String {
        val centerX = (left + right) / 2
        val centerY = (top + bottom) / 2

        val horizontal = when {
            centerX < 0.33f -> "on the left"
            centerX > 0.66f -> "on the right"
            else -> "in the center"
        }

        val vertical = when {
            centerY < 0.33f -> "near the top"
            centerY > 0.66f -> "near the bottom"
            else -> ""
        }

        return listOf(horizontal, vertical)
            .filter { it.isNotEmpty() }
            .joinToString(", ")
    }

    /**
     * Convert Bitmap to normalized float ByteBuffer.
     */
    private fun bitmapToFloatBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(
            1 * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE * FLOAT_SIZE
        )
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (pixel in pixels) {
            // Normalize to [-1, 1] range
            byteBuffer.putFloat(((pixel shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
            byteBuffer.putFloat(((pixel shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
            byteBuffer.putFloat(((pixel and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
        }

        byteBuffer.rewind()
        return byteBuffer
    }
}
