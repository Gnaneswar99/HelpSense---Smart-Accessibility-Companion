package com.accessai.core.model

import kotlinx.serialization.Serializable

/**
 * Represents the different accessibility modules in AccessAI.
 */
enum class AccessibilityModule {
    VISION,
    AUDIO,
    NAVIGATION,
    COMMUNICATION
}

/**
 * Result wrapper for ML inference operations.
 */
sealed class InferenceResult<out T> {
    data class Success<T>(val data: T, val confidenceScore: Float, val inferenceTimeMs: Long) : InferenceResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : InferenceResult<Nothing>()
    data object Loading : InferenceResult<Nothing>()
}

/**
 * Image captioning result from the Vision module.
 */
@Serializable
data class CaptionResult(
    val caption: String,
    val detectedObjects: List<DetectedObject> = emptyList(),
    val sceneType: String = "",
    val confidence: Float = 0f
)

/**
 * Detected object within an image.
 */
@Serializable
data class DetectedObject(
    val label: String,
    val confidence: Float,
    val boundingBox: BoundingBox? = null
)

/**
 * Bounding box coordinates (normalized 0-1).
 */
@Serializable
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

/**
 * Sound recognition result from the Audio module.
 */
@Serializable
data class SoundEvent(
    val soundClass: String,
    val confidence: Float,
    val timestamp: Long,
    val priority: SoundPriority = SoundPriority.NORMAL
)

/**
 * Priority levels for sound alerts.
 */
@Serializable
enum class SoundPriority {
    CRITICAL,   // Fire alarm, siren, car horn
    HIGH,       // Doorbell, phone ring, crying baby
    NORMAL,     // Speech, music
    LOW         // Background noise, wind
}

/**
 * Navigation waypoint for accessible route guidance.
 */
@Serializable
data class NavigationStep(
    val instruction: String,
    val distanceMeters: Double,
    val direction: String,
    val landmark: String? = null,
    val hasObstacle: Boolean = false,
    val obstacleDescription: String? = null
)

/**
 * User accessibility preferences.
 */
@Serializable
data class AccessibilityPreferences(
    val ttsSpeed: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val hapticFeedbackEnabled: Boolean = true,
    val highContrastMode: Boolean = false,
    val soundAlertProfiles: Map<String, Boolean> = defaultSoundAlerts(),
    val preferredLanguage: String = "en"
) {
    companion object {
        fun defaultSoundAlerts() = mapOf(
            "fire_alarm" to true,
            "doorbell" to true,
            "car_horn" to true,
            "siren" to true,
            "baby_cry" to true,
            "phone_ring" to true,
            "speech" to false,
            "dog_bark" to true
        )
    }
}
