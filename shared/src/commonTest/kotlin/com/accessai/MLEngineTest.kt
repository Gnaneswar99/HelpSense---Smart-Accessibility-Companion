package com.accessai

import com.accessai.core.ml.ModelInfo
import com.accessai.core.model.CaptionResult
import com.accessai.core.model.DetectedObject
import com.accessai.core.model.BoundingBox
import com.accessai.core.model.InferenceResult
import com.accessai.core.model.SoundEvent
import com.accessai.core.model.SoundPriority
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MLEngineTest {

    @Test
    fun `ModelInfo tracks loaded state`() {
        val info = ModelInfo(
            name = "detect.tflite",
            version = "1.0",
            sizeBytes = 4_000_000,
            inputShape = listOf(1, 300, 300, 3),
            isLoaded = true,
            inferenceTimeAvgMs = 35
        )

        assertEquals("detect.tflite", info.name)
        assertTrue(info.isLoaded)
        assertEquals(listOf(1, 300, 300, 3), info.inputShape)
    }

    @Test
    fun `CaptionResult contains scene and objects`() {
        val objects = listOf(
            DetectedObject("person", 0.92f, BoundingBox(0.1f, 0.2f, 0.5f, 0.8f)),
            DetectedObject("dog", 0.87f, BoundingBox(0.6f, 0.4f, 0.9f, 0.9f))
        )

        val caption = CaptionResult(
            caption = "I see a park. I can see a person and a dog.",
            detectedObjects = objects,
            sceneType = "park",
            confidence = 0.85f
        )

        assertEquals(2, caption.detectedObjects.size)
        assertEquals("park", caption.sceneType)
        assertTrue(caption.caption.contains("person"))
        assertTrue(caption.caption.contains("dog"))
    }

    @Test
    fun `InferenceResult Success carries inference timing`() {
        val result = InferenceResult.Success(
            data = SoundEvent("fire_alarm", 0.95f, 1000L, SoundPriority.CRITICAL),
            confidenceScore = 0.95f,
            inferenceTimeMs = 12L
        )

        assertEquals(12L, result.inferenceTimeMs)
        assertEquals(SoundPriority.CRITICAL, result.data.priority)
    }

    @Test
    fun `SoundEvent priority mapping is correct for critical sounds`() {
        val criticalSounds = listOf("fire_alarm", "siren", "car_horn")
        criticalSounds.forEach { sound ->
            val event = SoundEvent(sound, 0.9f, 1000L, SoundPriority.CRITICAL)
            assertEquals(SoundPriority.CRITICAL, event.priority,
                "Expected CRITICAL priority for $sound")
        }
    }

    @Test
    fun `DetectedObject sorting by confidence works`() {
        val objects = listOf(
            DetectedObject("chair", 0.6f),
            DetectedObject("person", 0.95f),
            DetectedObject("table", 0.75f)
        )

        val sorted = objects.sortedByDescending { it.confidence }
        assertEquals("person", sorted[0].label)
        assertEquals("table", sorted[1].label)
        assertEquals("chair", sorted[2].label)
    }
}
