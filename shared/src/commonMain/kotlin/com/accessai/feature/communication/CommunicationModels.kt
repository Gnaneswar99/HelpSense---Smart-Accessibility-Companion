package com.accessai.feature.communication

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * Quick phrase categories for fast communication.
 */
@Serializable
enum class PhraseCategory {
    GREETINGS,
    NEEDS,
    EMERGENCY,
    DIRECTIONS,
    SHOPPING,
    MEDICAL,
    CUSTOM
}

/**
 * A pre-built phrase for quick communication.
 */
@Serializable
data class QuickPhrase(
    val id: String,
    val text: String,
    val category: PhraseCategory,
    val emoji: String = "",
    val isCustom: Boolean = false
)

/**
 * Speech recognition result.
 */
@Serializable
data class SpeechResult(
    val text: String,
    val confidence: Float,
    val isFinal: Boolean
)

/**
 * Conversation message in the communication log.
 */
@Serializable
data class ConversationMessage(
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val wasSpoken: Boolean = false
)

/**
 * Default quick phrases organized by category.
 * These cover the most common needs for accessibility users.
 */
object DefaultPhrases {

    fun getAll(): List<QuickPhrase> = greetings + needs + emergency + directions + shopping + medical

    val greetings = listOf(
        QuickPhrase("g1", "Hello, how are you?", PhraseCategory.GREETINGS, "\uD83D\uDC4B"),
        QuickPhrase("g2", "Thank you very much", PhraseCategory.GREETINGS, "\uD83D\uDE4F"),
        QuickPhrase("g3", "Nice to meet you", PhraseCategory.GREETINGS, "\uD83D\uDE04"),
        QuickPhrase("g4", "Goodbye, have a nice day", PhraseCategory.GREETINGS, "\uD83D\uDC4B"),
        QuickPhrase("g5", "Excuse me", PhraseCategory.GREETINGS, "\uD83D\uDE4F"),
        QuickPhrase("g6", "Yes please", PhraseCategory.GREETINGS, "\u2705"),
        QuickPhrase("g7", "No thank you", PhraseCategory.GREETINGS, "\u274C"),
    )

    val needs = listOf(
        QuickPhrase("n1", "I need help please", PhraseCategory.NEEDS, "\uD83C\uDD98"),
        QuickPhrase("n2", "Can you speak more slowly?", PhraseCategory.NEEDS, "\uD83D\uDD07"),
        QuickPhrase("n3", "Can you write that down for me?", PhraseCategory.NEEDS, "\u270D\uFE0F"),
        QuickPhrase("n4", "I am deaf and use this app to communicate", PhraseCategory.NEEDS, "\uD83E\uDDBB"),
        QuickPhrase("n5", "I am blind and need assistance", PhraseCategory.NEEDS, "\uD83E\uDDAF"),
        QuickPhrase("n6", "Can you repeat that?", PhraseCategory.NEEDS, "\uD83D\uDD01"),
        QuickPhrase("n7", "I need to sit down", PhraseCategory.NEEDS, "\uD83E\uDE91"),
        QuickPhrase("n8", "Where is the restroom?", PhraseCategory.NEEDS, "\uD83D\uDEBB"),
    )

    val emergency = listOf(
        QuickPhrase("e1", "Please call 911", PhraseCategory.EMERGENCY, "\uD83D\uDEA8"),
        QuickPhrase("e2", "I need medical help", PhraseCategory.EMERGENCY, "\uD83C\uDFE5"),
        QuickPhrase("e3", "I am lost, please help me", PhraseCategory.EMERGENCY, "\uD83C\uDD98"),
        QuickPhrase("e4", "I am having an allergic reaction", PhraseCategory.EMERGENCY, "\u26A0\uFE0F"),
        QuickPhrase("e5", "My emergency contact number is on this phone", PhraseCategory.EMERGENCY, "\uD83D\uDCF1"),
    )

    val directions = listOf(
        QuickPhrase("d1", "Where is the nearest bus stop?", PhraseCategory.DIRECTIONS, "\uD83D\uDE8C"),
        QuickPhrase("d2", "Is there an elevator nearby?", PhraseCategory.DIRECTIONS, "\uD83D\uDED7"),
        QuickPhrase("d3", "Which way to the exit?", PhraseCategory.DIRECTIONS, "\uD83D\uDEAA"),
        QuickPhrase("d4", "Is this path wheelchair accessible?", PhraseCategory.DIRECTIONS, "\u267F"),
        QuickPhrase("d5", "How far is it to walk?", PhraseCategory.DIRECTIONS, "\uD83D\uDEB6"),
    )

    val shopping = listOf(
        QuickPhrase("s1", "How much does this cost?", PhraseCategory.SHOPPING, "\uD83D\uDCB0"),
        QuickPhrase("s2", "Can you help me find this item?", PhraseCategory.SHOPPING, "\uD83D\uDD0D"),
        QuickPhrase("s3", "I would like to pay with card", PhraseCategory.SHOPPING, "\uD83D\uDCB3"),
        QuickPhrase("s4", "Do you have this in a different size?", PhraseCategory.SHOPPING, "\uD83D\uDC55"),
    )

    val medical = listOf(
        QuickPhrase("m1", "I take the following medications", PhraseCategory.MEDICAL, "\uD83D\uDC8A"),
        QuickPhrase("m2", "I have a disability", PhraseCategory.MEDICAL, "\u267F"),
        QuickPhrase("m3", "I have allergies to", PhraseCategory.MEDICAL, "\u26A0\uFE0F"),
        QuickPhrase("m4", "My blood type is", PhraseCategory.MEDICAL, "\uD83E\uDE78"),
        QuickPhrase("m5", "Please contact my doctor", PhraseCategory.MEDICAL, "\uD83D\uDC68\u200D\u2695\uFE0F"),
    )
}
