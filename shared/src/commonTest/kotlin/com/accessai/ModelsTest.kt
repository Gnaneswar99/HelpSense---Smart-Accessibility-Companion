package com.accessai

import com.accessai.core.model.AccessibilityPreferences
import com.accessai.core.model.BoundingBox
import com.accessai.core.model.CaptionResult
import com.accessai.core.model.DetectedObject
import com.accessai.core.model.InferenceResult
import com.accessai.core.model.SoundEvent
import com.accessai.core.model.SoundPriority
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ModelsTest {

    @Test
    fun `InferenceResult Success carries data and metadata`() {
        val result = InferenceResult.Success(
            data = CaptionResult(
                caption = "A person walking on a sidewalk",
                confidence = 0.92f
            ),
            confidenceScore = 0.92f,
            inferenceTimeMs = 45L
        )

        assertIs<InferenceResult.Success<CaptionResult>>(result)
        assertEquals("A person walking on a sidewalk", result.data.caption)
        assertEquals(0.92f, result.confidenceScore)
        assertEquals(45L, result.inferenceTimeMs)
    }

    @Test
    fun `InferenceResult Error carries message`() {
        val result = InferenceResult.Error("Model not loaded")

        assertIs<InferenceResult.Error>(result)
        assertEquals("Model not loaded", result.message)
    }

    @Test
    fun `DetectedObject contains label and confidence`() {
        val obj = DetectedObject(
            label = "car",
            confidence = 0.87f,
            boundingBox = BoundingBox(0.1f, 0.2f, 0.5f, 0.6f)
        )

        assertEquals("car", obj.label)
        assertEquals(0.87f, obj.confidence)
        assertTrue(obj.boundingBox!!.right > obj.boundingBox!!.left)
    }

    @Test
    fun `SoundEvent priority classification works`() {
        val critical = SoundEvent("fire_alarm", 0.95f, 1000L, SoundPriority.CRITICAL)
        val normal = SoundEvent("speech", 0.80f, 2000L, SoundPriority.NORMAL)

        assertEquals(SoundPriority.CRITICAL, critical.priority)
        assertEquals(SoundPriority.NORMAL, normal.priority)
    }

    @Test
    fun `Default accessibility preferences are sensible`() {
        val prefs = AccessibilityPreferences()

        assertEquals(1.0f, prefs.ttsSpeed)
        assertEquals(1.0f, prefs.ttsPitch)
        assertTrue(prefs.hapticFeedbackEnabled)
        assertEquals("en", prefs.preferredLanguage)
        // Fire alarm should be enabled by default
        assertTrue(prefs.soundAlertProfiles["fire_alarm"] == true)
        // Speech alerts off by default (too noisy)
        assertTrue(prefs.soundAlertProfiles["speech"] == false)
    }
}
