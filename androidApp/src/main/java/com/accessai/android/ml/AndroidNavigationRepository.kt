package com.accessai.android.ml

import com.accessai.core.model.DetectedObject
import com.accessai.core.model.InferenceResult
import com.accessai.core.model.NavigationRepository
import com.accessai.core.model.NavigationStep

/**
 * Android implementation of NavigationRepository.
 *
 * Combines:
 * - Object detection for obstacle awareness
 * - Route planning with accessibility-friendly directions
 *
 * Route planning uses offline step generation for now.
 * A real implementation would integrate Google Directions API
 * with accessible route preferences.
 */
class AndroidNavigationRepository(
    private val objectDetectionEngine: TFLiteObjectDetectionEngine
) : NavigationRepository {

    override suspend fun getAccessibleRoute(
        originLat: Double, originLng: Double,
        destLat: Double, destLng: Double
    ): InferenceResult<List<NavigationStep>> {
        return try {
            // Calculate straight-line distance for demo
            val distanceKm = haversineDistance(originLat, originLng, destLat, destLng)
            val distanceMeters = distanceKm * 1000

            // Generate accessible navigation steps
            // In production, this would call Google Directions API with:
            // - mode=walking
            // - avoid=stairs (custom accessibility filter)
            // - prefer=sidewalks, crosswalks with signals
            val steps = generateAccessibleSteps(
                originLat, originLng,
                destLat, destLng,
                distanceMeters
            )

            InferenceResult.Success(
                data = steps,
                confidenceScore = 1.0f,
                inferenceTimeMs = 50L
            )
        } catch (e: Exception) {
            InferenceResult.Error("Route planning failed: ${e.message}", e)
        }
    }

    override suspend fun detectObstacles(imageBytes: ByteArray): InferenceResult<List<DetectedObject>> {
        // Reuse object detection engine with lower threshold for safety
        return objectDetectionEngine.detect(
            imageBytes,
            maxResults = 15,
            confidenceThreshold = 0.35f  // Lower threshold = more cautious
        )
    }

    /**
     * Generate step-by-step accessible navigation directions.
     * In production, replace with Google Directions API integration.
     */
    private fun generateAccessibleSteps(
        originLat: Double, originLng: Double,
        destLat: Double, destLng: Double,
        totalDistanceMeters: Double
    ): List<NavigationStep> {
        val bearing = calculateBearing(originLat, originLng, destLat, destLng)
        val direction = bearingToDirection(bearing)
        val steps = mutableListOf<NavigationStep>()

        // Start step
        steps.add(
            NavigationStep(
                instruction = "Start walking $direction",
                distanceMeters = 0.0,
                direction = direction,
                landmark = "Starting point"
            )
        )

        // Generate intermediate steps based on distance
        val numSteps = when {
            totalDistanceMeters < 200 -> 2
            totalDistanceMeters < 500 -> 3
            totalDistanceMeters < 1000 -> 4
            else -> 5
        }

        val stepDistance = totalDistanceMeters / numSteps

        for (i in 1 until numSteps) {
            val stepDirections = listOf(
                "Continue straight $direction",
                "Keep walking along the sidewalk",
                "Cross at the next intersection with traffic signal",
                "Continue past the next block",
                "Walk along the accessible path"
            )

            steps.add(
                NavigationStep(
                    instruction = stepDirections[i % stepDirections.size],
                    distanceMeters = stepDistance,
                    direction = direction,
                    landmark = getGenericLandmark(i)
                )
            )
        }

        // Arrival step
        steps.add(
            NavigationStep(
                instruction = "You have arrived at your destination",
                distanceMeters = stepDistance,
                direction = "arrived",
                landmark = "Destination"
            )
        )

        return steps
    }

    private fun getGenericLandmark(index: Int): String {
        val landmarks = listOf(
            "Intersection ahead",
            "Crosswalk with signal",
            "Building entrance on your right",
            "Park area on your left",
            "Bus stop nearby"
        )
        return landmarks[index % landmarks.size]
    }

    /**
     * Haversine formula for distance between two GPS coordinates.
     */
    private fun haversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6371.0 // Earth's radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    /**
     * Calculate bearing between two GPS points.
     */
    private fun calculateBearing(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val y = Math.sin(dLon) * Math.cos(lat2Rad)
        val x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLon)
        val bearing = Math.toDegrees(Math.atan2(y, x))
        return (bearing + 360) % 360
    }

    private fun bearingToDirection(bearing: Double): String = when {
        bearing < 22.5 || bearing >= 337.5 -> "north"
        bearing < 67.5 -> "northeast"
        bearing < 112.5 -> "east"
        bearing < 157.5 -> "southeast"
        bearing < 202.5 -> "south"
        bearing < 247.5 -> "southwest"
        bearing < 292.5 -> "west"
        else -> "northwest"
    }
}
