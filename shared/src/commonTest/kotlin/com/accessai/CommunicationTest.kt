package com.accessai

import com.accessai.feature.communication.ConversationMessage
import com.accessai.feature.communication.DefaultPhrases
import com.accessai.feature.communication.PhraseCategory
import com.accessai.feature.communication.SpeechResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommunicationTest {

    @Test
    fun `DefaultPhrases covers all non-custom categories`() {
        val allPhrases = DefaultPhrases.getAll()
        assertTrue(allPhrases.isNotEmpty())

        // Each category should have at least 4 phrases
        assertTrue(DefaultPhrases.greetings.size >= 4)
        assertTrue(DefaultPhrases.needs.size >= 4)
        assertTrue(DefaultPhrases.emergency.size >= 3)
        assertTrue(DefaultPhrases.directions.size >= 3)
        assertTrue(DefaultPhrases.shopping.size >= 3)
        assertTrue(DefaultPhrases.medical.size >= 3)
    }

    @Test
    fun `QuickPhrase IDs are unique within category`() {
        val allPhrases = DefaultPhrases.getAll()
        val ids = allPhrases.map { it.id }
        assertEquals(ids.size, ids.distinct().size, "Duplicate phrase IDs found")
    }

    @Test
    fun `Emergency phrases are categorized correctly`() {
        DefaultPhrases.emergency.forEach { phrase ->
            assertEquals(PhraseCategory.EMERGENCY, phrase.category)
        }
    }

    @Test
    fun `SpeechResult distinguishes partial from final`() {
        val partial = SpeechResult("Hello, I'm", 0.5f, isFinal = false)
        val final = SpeechResult("Hello, I'm looking for help", 0.92f, isFinal = true)

        assertFalse(partial.isFinal)
        assertTrue(final.isFinal)
        assertTrue(final.confidence > partial.confidence)
    }

    @Test
    fun `ConversationMessage tracks sender correctly`() {
        val userMsg = ConversationMessage(
            text = "Where is the elevator?",
            isFromUser = true,
            timestamp = 1000L,
            wasSpoken = true
        )

        val otherMsg = ConversationMessage(
            text = "It's down the hall on the left",
            isFromUser = false,
            timestamp = 2000L,
            wasSpoken = false
        )

        assertTrue(userMsg.isFromUser)
        assertTrue(userMsg.wasSpoken)
        assertFalse(otherMsg.isFromUser)
        assertFalse(otherMsg.wasSpoken)
    }

    @Test
    fun `All phrases have non-empty text`() {
        DefaultPhrases.getAll().forEach { phrase ->
            assertTrue(phrase.text.isNotBlank(), "Phrase ${phrase.id} has empty text")
        }
    }

    @Test
    fun `Emergency phrases contain urgent language`() {
        val emergencyTexts = DefaultPhrases.emergency.map { it.text.lowercase() }
        // At least one should contain "911", "help", or "emergency"
        assertTrue(
            emergencyTexts.any {
                it.contains("911") || it.contains("help") || it.contains("emergency") || it.contains("medical")
            },
            "Emergency phrases should contain urgent language"
        )
    }
}
