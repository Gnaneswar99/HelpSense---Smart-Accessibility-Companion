package com.accessai.feature.navigation

import com.accessai.core.model.DetectedObject
import com.accessai.core.model.InferenceResult
import com.accessai.core.model.NavigationStep
import com.accessai.core.model.NavigationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable

/**
 * Navigation session state tracked across the journey.
 */
@Serializable
data class NavigationSession(
    val originLat: Double,
    val originLng: Double,
    val destinationLat: Double,
    val destinationLng: Double,
    val destinationName: String = "",
    val steps: List<NavigationStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val distanceRemainingMeters: Double = 0.0,
    val isActive: Boolean = false
) {
    val currentStep: NavigationStep?
        get() = steps.getOrNull(currentStepIndex)

    val isComplete: Boolean
        get() = currentStepIndex >= steps.size

    val progressPercent: Float
        get() = if (steps.isEmpty()) 0f else (currentStepIndex.toFloat() / steps.size) * 100f
}

/**
 * Obstacle detected in the camera frame during navigation.
 */
@Serializable
data class Obstacle(
    val type: ObstacleType,
    val label: String,
    val confidence: Float,
    val distanceEstimate: String,  // "near", "medium", "far"
    val direction: String           // "ahead", "left", "right"
)

/**
 * Categories of obstacles relevant for accessible navigation.
 */
@Serializable
enum class ObstacleType {
    VEHICLE,        // Car, truck, bus, motorcycle
    PERSON,         // Pedestrian in path
    BICYCLE,        // Cyclist
    CONSTRUCTION,   // Barriers, cones, signs
    FURNITURE,      // Bench, bollard, pole
    STEP_CURB,      // Steps, curbs, elevation changes
    ANIMAL,         // Dog, etc.
    OTHER           // Unclassified obstacle
}

/**
 * Use case for getting accessible navigation routes.
 */
class GetAccessibleRouteUseCase(
    private val navigationRepository: NavigationRepository
) {
    suspend operator fun invoke(
        originLat: Double, originLng: Double,
        destLat: Double, destLng: Double
    ): InferenceResult<List<NavigationStep>> {
        return navigationRepository.getAccessibleRoute(
            originLat, originLng, destLat, destLng
        )
    }
}

/**
 * Use case for detecting obstacles in the camera frame during navigation.
 */
class DetectObstaclesUseCase(
    private val navigationRepository: NavigationRepository
) {
    suspend operator fun invoke(imageBytes: ByteArray): InferenceResult<List<DetectedObject>> {
        if (imageBytes.isEmpty()) {
            return InferenceResult.Error("Image data is empty")
        }
        return navigationRepository.detectObstacles(imageBytes)
    }
}

/**
 * Use case for continuous obstacle monitoring during active navigation.
 * Processes camera frames and maps detected objects to obstacle warnings.
 */
class MonitorObstaclesUseCase(
    private val navigationRepository: NavigationRepository
) {
    /**
     * Continuously detect obstacles from camera frames.
     * @param frameProvider Function that returns the latest camera frame bytes
     * @param intervalMs How often to check (default 500ms for battery efficiency)
     */
    fun monitor(
        frameProvider: suspend () -> ByteArray,
        intervalMs: Long = 500
    ): Flow<List<Obstacle>> = flow {
        while (true) {
            try {
                val frameBytes = frameProvider()
                val result = navigationRepository.detectObstacles(frameBytes)

                when (result) {
                    is InferenceResult.Success -> {
                        val obstacles = result.data.mapNotNull { obj ->
                            mapToObstacle(obj)
                        }
                        if (obstacles.isNotEmpty()) {
                            emit(obstacles)
                        }
                    }
                    else -> { /* Skip frame on error */ }
                }
            } catch (e: Exception) {
                // Continue monitoring even if one frame fails
            }

            kotlinx.coroutines.delay(intervalMs)
        }
    }

    /**
     * Map a detected object to a navigation obstacle with spatial info.
     */
    private fun mapToObstacle(obj: DetectedObject): Obstacle? {
        val type = classifyObstacleType(obj.label)
        val box = obj.boundingBox ?: return null

        // Estimate distance from bounding box size (larger box = closer)
        val boxArea = (box.right - box.left) * (box.bottom - box.top)
        val distanceEstimate = when {
            boxArea > 0.25f -> "near"       // Object fills >25% of frame
            boxArea > 0.08f -> "medium"     // 8-25%
            else -> "far"                   // <8%
        }

        // Estimate direction from horizontal center
        val centerX = (box.left + box.right) / 2
        val direction = when {
            centerX < 0.35f -> "left"
            centerX > 0.65f -> "right"
            else -> "ahead"
        }

        return Obstacle(
            type = type,
            label = obj.label,
            confidence = obj.confidence,
            distanceEstimate = distanceEstimate,
            direction = direction
        )
    }

    private fun classifyObstacleType(label: String): ObstacleType {
        val l = label.lowercase()
        return when {
            l.contains("car") || l.contains("truck") || l.contains("bus") ||
                    l.contains("motorcycle") || l.contains("vehicle") -> ObstacleType.VEHICLE
            l.contains("person") || l.contains("pedestrian") -> ObstacleType.PERSON
            l.contains("bicycle") || l.contains("bike") -> ObstacleType.BICYCLE
            l.contains("cone") || l.contains("barrier") || l.contains("construction") -> ObstacleType.CONSTRUCTION
            l.contains("bench") || l.contains("pole") || l.contains("hydrant") ||
                    l.contains("bollard") || l.contains("chair") -> ObstacleType.FURNITURE
            l.contains("dog") || l.contains("cat") || l.contains("animal") -> ObstacleType.ANIMAL
            else -> ObstacleType.OTHER
        }
    }
}
