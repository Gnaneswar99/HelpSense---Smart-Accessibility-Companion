package com.accessai

import com.accessai.core.model.NavigationStep
import com.accessai.feature.navigation.NavigationSession
import com.accessai.feature.navigation.Obstacle
import com.accessai.feature.navigation.ObstacleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NavigationTest {

    @Test
    fun `NavigationSession tracks progress correctly`() {
        val steps = listOf(
            NavigationStep("Start walking north", 0.0, "north"),
            NavigationStep("Continue straight", 100.0, "north"),
            NavigationStep("Turn right", 50.0, "east"),
            NavigationStep("You have arrived", 30.0, "arrived")
        )

        val session = NavigationSession(
            originLat = 35.68,
            originLng = 139.76,
            destinationLat = 35.685,
            destinationLng = 139.763,
            destinationName = "Tokyo Station",
            steps = steps,
            currentStepIndex = 0,
            isActive = true
        )

        assertEquals(4, session.steps.size)
        assertFalse(session.isComplete)
        assertEquals(0f, session.progressPercent)
        assertNotNull(session.currentStep)
        assertEquals("Start walking north", session.currentStep?.instruction)
    }

    @Test
    fun `NavigationSession detects completion`() {
        val steps = listOf(
            NavigationStep("Walk north", 100.0, "north"),
            NavigationStep("Arrived", 0.0, "arrived")
        )

        val session = NavigationSession(
            originLat = 0.0, originLng = 0.0,
            destinationLat = 0.0, destinationLng = 0.0,
            steps = steps,
            currentStepIndex = 2  // Past the last step
        )

        assertTrue(session.isComplete)
        assertNull(session.currentStep)
        assertEquals(100f, session.progressPercent)
    }

    @Test
    fun `NavigationSession progress calculation`() {
        val steps = List(4) {
            NavigationStep("Step ${it + 1}", 100.0, "north")
        }

        val session = NavigationSession(
            originLat = 0.0, originLng = 0.0,
            destinationLat = 0.0, destinationLng = 0.0,
            steps = steps,
            currentStepIndex = 2
        )

        assertEquals(50f, session.progressPercent) // 2/4 = 50%
    }

    @Test
    fun `Obstacle types are classified correctly`() {
        val vehicleObstacle = Obstacle(
            type = ObstacleType.VEHICLE,
            label = "car",
            confidence = 0.9f,
            distanceEstimate = "near",
            direction = "ahead"
        )

        assertEquals(ObstacleType.VEHICLE, vehicleObstacle.type)
        assertEquals("near", vehicleObstacle.distanceEstimate)
        assertEquals("ahead", vehicleObstacle.direction)
    }

    @Test
    fun `Obstacle distance estimates are valid values`() {
        val validDistances = listOf("near", "medium", "far")

        val obstacle = Obstacle(
            type = ObstacleType.PERSON,
            label = "person",
            confidence = 0.85f,
            distanceEstimate = "near",
            direction = "left"
        )

        assertTrue(obstacle.distanceEstimate in validDistances)
    }

    @Test
    fun `NavigationStep with landmark info`() {
        val step = NavigationStep(
            instruction = "Cross at the intersection",
            distanceMeters = 50.0,
            direction = "north",
            landmark = "Traffic light with audio signal",
            hasObstacle = true,
            obstacleDescription = "Construction barrier on right side"
        )

        assertNotNull(step.landmark)
        assertTrue(step.hasObstacle)
        assertNotNull(step.obstacleDescription)
    }

    @Test
    fun `Empty NavigationSession handles edge cases`() {
        val emptySession = NavigationSession(
            originLat = 0.0, originLng = 0.0,
            destinationLat = 0.0, destinationLng = 0.0,
            steps = emptyList()
        )

        assertTrue(emptySession.isComplete) // No steps = already there
        assertNull(emptySession.currentStep)
        assertEquals(0f, emptySession.progressPercent)
    }
}
