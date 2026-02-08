package com.accessai.core.model

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Vision module operations.
 * Platform-specific implementations handle TFLite (Android) and CoreML (iOS).
 */
interface VisionRepository {
    /**
     * Generate a caption for the given image bytes.
     */
    suspend fun captionImage(imageBytes: ByteArray): InferenceResult<CaptionResult>

    /**
     * Detect objects in the given image bytes.
     */
    suspend fun detectObjects(imageBytes: ByteArray): InferenceResult<List<DetectedObject>>

    /**
     * Extract text from an image via OCR.
     */
    suspend fun extractText(imageBytes: ByteArray): InferenceResult<String>

    /**
     * Stream of continuous captioning results (for live camera mode).
     */
    fun continuousCaptioning(): Flow<InferenceResult<CaptionResult>>
}

/**
 * Repository interface for Audio module operations.
 */
interface AudioRepository {
    /**
     * Start listening for sound events.
     */
    fun startListening(): Flow<InferenceResult<SoundEvent>>

    /**
     * Stop the audio listener.
     */
    suspend fun stopListening()

    /**
     * Check if the audio monitor is currently active.
     */
    fun isListening(): Boolean
}

/**
 * Repository interface for Navigation module operations.
 */
interface NavigationRepository {
    /**
     * Get accessible navigation steps from origin to destination.
     */
    suspend fun getAccessibleRoute(
        originLat: Double, originLng: Double,
        destLat: Double, destLng: Double
    ): InferenceResult<List<NavigationStep>>

    /**
     * Detect obstacles in the current camera frame.
     */
    suspend fun detectObstacles(imageBytes: ByteArray): InferenceResult<List<DetectedObject>>
}

/**
 * Repository interface for user preferences.
 */
interface PreferencesRepository {
    /**
     * Get current accessibility preferences.
     */
    suspend fun getPreferences(): AccessibilityPreferences

    /**
     * Update accessibility preferences.
     */
    suspend fun updatePreferences(preferences: AccessibilityPreferences)

    /**
     * Observe preference changes as a Flow.
     */
    fun observePreferences(): Flow<AccessibilityPreferences>
}
