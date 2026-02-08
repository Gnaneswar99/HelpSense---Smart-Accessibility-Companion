package com.accessai.core.ml

import com.accessai.core.model.CaptionResult
import com.accessai.core.model.DetectedObject
import com.accessai.core.model.InferenceResult
import com.accessai.core.model.SoundEvent
import kotlinx.coroutines.flow.Flow

/**
 * Abstract ML inference engine.
 * Platform implementations wrap TFLite (Android) / CoreML (iOS).
 */
interface MLEngine {
    /**
     * Initialize all ML models. Should be called once at app startup.
     * @return true if all models loaded successfully
     */
    suspend fun initialize(): Boolean

    /**
     * Check if models are loaded and ready.
     */
    fun isReady(): Boolean

    /**
     * Release all model resources.
     */
    fun release()
}

/**
 * Image captioning inference engine.
 */
interface ImageCaptionEngine {
    /**
     * Generate a text caption for the given image.
     * @param imageBytes JPEG-encoded image
     * @return Caption with confidence score
     */
    suspend fun caption(imageBytes: ByteArray): InferenceResult<CaptionResult>
}

/**
 * Object detection inference engine.
 */
interface ObjectDetectionEngine {
    /**
     * Detect objects in the given image.
     * @param imageBytes JPEG-encoded image
     * @param maxResults Maximum number of detections to return
     * @param confidenceThreshold Minimum confidence (0-1) to include
     * @return List of detected objects with bounding boxes
     */
    suspend fun detect(
        imageBytes: ByteArray,
        maxResults: Int = 10,
        confidenceThreshold: Float = 0.5f
    ): InferenceResult<List<DetectedObject>>
}

/**
 * Sound classification inference engine.
 */
interface SoundClassificationEngine {
    /**
     * Classify a sound from raw audio data.
     * @param audioData PCM 16-bit mono audio samples
     * @param sampleRate Sample rate of the audio (typically 16000)
     * @return Classified sound event with confidence
     */
    suspend fun classify(
        audioData: ShortArray,
        sampleRate: Int = 16000
    ): InferenceResult<SoundEvent>
}

/**
 * OCR (Optical Character Recognition) engine.
 */
interface OCREngine {
    /**
     * Extract text from an image.
     * @param imageBytes JPEG-encoded image
     * @return Extracted text string
     */
    suspend fun extractText(imageBytes: ByteArray): InferenceResult<String>
}

/**
 * Model metadata for tracking loaded models.
 */
data class ModelInfo(
    val name: String,
    val version: String,
    val sizeBytes: Long,
    val inputShape: List<Int>,
    val isLoaded: Boolean = false,
    val inferenceTimeAvgMs: Long = 0
)
