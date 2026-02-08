package com.accessai.android.ui.screens.communication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accessai.android.service.AndroidSpeechRecognitionService
import com.accessai.core.util.HapticService
import com.accessai.core.util.TextToSpeechService
import com.accessai.feature.communication.ConversationMessage
import com.accessai.feature.communication.DefaultPhrases
import com.accessai.feature.communication.PhraseCategory
import com.accessai.feature.communication.QuickPhrase
import com.accessai.feature.communication.SpeechResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Communication screen.
 */
data class CommunicationUiState(
    // Text input
    val typedText: String = "",

    // Speech recognition
    val isListening: Boolean = false,
    val partialSpeechText: String = "",

    // Speaking
    val isSpeaking: Boolean = false,

    // Quick phrases
    val selectedCategory: PhraseCategory = PhraseCategory.GREETINGS,
    val phrases: List<QuickPhrase> = DefaultPhrases.greetings,
    val allCategories: List<PhraseCategory> = PhraseCategory.values().toList().filter { it != PhraseCategory.CUSTOM },

    // Conversation log
    val conversation: List<ConversationMessage> = emptyList(),

    // Display mode
    val showQuickPhrases: Boolean = true,

    // Status
    val statusMessage: String = "Type or tap a phrase to speak aloud"
)

/**
 * CommunicationViewModel — powers the Communication module.
 *
 * Two-way communication:
 * 1. User → World: Type text or tap quick phrases → TTS speaks them aloud
 * 2. World → User: Speech recognition captures what others say → shows on screen
 *
 * Also maintains a conversation log so the user can review the exchange.
 */
class CommunicationViewModel(
    private val ttsService: TextToSpeechService,
    private val speechRecognitionService: AndroidSpeechRecognitionService,
    private val hapticService: HapticService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunicationUiState())
    val uiState: StateFlow<CommunicationUiState> = _uiState.asStateFlow()

    private val conversationLog = mutableListOf<ConversationMessage>()

    /**
     * Update the typed text field.
     */
    fun updateText(text: String) {
        _uiState.value = _uiState.value.copy(typedText = text)
    }

    /**
     * Speak the typed text aloud and add to conversation log.
     */
    fun speakTypedText() {
        val text = _uiState.value.typedText.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            addToConversation(text, isFromUser = true, wasSpoken = true)
            _uiState.value = _uiState.value.copy(
                isSpeaking = true,
                typedText = "",
                statusMessage = "Speaking: \"$text\""
            )

            ttsService.speak(text)
            hapticService.lightTap()

            _uiState.value = _uiState.value.copy(
                isSpeaking = false,
                statusMessage = "Finished speaking"
            )
        }
    }

    /**
     * Speak a quick phrase aloud.
     */
    fun speakPhrase(phrase: QuickPhrase) {
        viewModelScope.launch {
            addToConversation(phrase.text, isFromUser = true, wasSpoken = true)
            _uiState.value = _uiState.value.copy(
                isSpeaking = true,
                statusMessage = "Speaking: \"${phrase.text}\""
            )

            ttsService.speak(phrase.text)
            hapticService.lightTap()

            _uiState.value = _uiState.value.copy(
                isSpeaking = false,
                statusMessage = "Finished speaking"
            )
        }
    }

    /**
     * Start listening to someone speaking (Speech-to-Text).
     * Shows what they say on screen for the user.
     */
    fun startListening() {
        if (!speechRecognitionService.isAvailable()) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Speech recognition not available on this device"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isListening = true,
                partialSpeechText = "",
                statusMessage = "Listening... Have the other person speak now"
            )
            hapticService.lightTap()

            speechRecognitionService.startListening().collect { result ->
                handleSpeechResult(result)
            }
        }
    }

    /**
     * Stop listening.
     */
    fun stopListening() {
        speechRecognitionService.stopListening()
        _uiState.value = _uiState.value.copy(
            isListening = false,
            statusMessage = "Stopped listening"
        )
    }

    private fun handleSpeechResult(result: SpeechResult) {
        if (result.isFinal) {
            // Final result — add to conversation log
            if (result.text.isNotBlank() && result.confidence > 0.1f) {
                addToConversation(result.text, isFromUser = false, wasSpoken = false)
            }
            _uiState.value = _uiState.value.copy(
                isListening = false,
                partialSpeechText = "",
                statusMessage = if (result.confidence > 0.1f) "They said: \"${result.text}\""
                else result.text  // Error message
            )
        } else {
            // Partial result — show live transcription
            _uiState.value = _uiState.value.copy(
                partialSpeechText = result.text
            )
        }
    }

    /**
     * Switch quick phrase category.
     */
    fun selectCategory(category: PhraseCategory) {
        val phrases = when (category) {
            PhraseCategory.GREETINGS -> DefaultPhrases.greetings
            PhraseCategory.NEEDS -> DefaultPhrases.needs
            PhraseCategory.EMERGENCY -> DefaultPhrases.emergency
            PhraseCategory.DIRECTIONS -> DefaultPhrases.directions
            PhraseCategory.SHOPPING -> DefaultPhrases.shopping
            PhraseCategory.MEDICAL -> DefaultPhrases.medical
            PhraseCategory.CUSTOM -> emptyList()
        }

        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            phrases = phrases
        )
    }

    /**
     * Toggle between quick phrases view and conversation log.
     */
    fun toggleView() {
        _uiState.value = _uiState.value.copy(
            showQuickPhrases = !_uiState.value.showQuickPhrases
        )
    }

    /**
     * Stop TTS playback.
     */
    fun stopSpeaking() {
        ttsService.stop()
        _uiState.value = _uiState.value.copy(isSpeaking = false)
    }

    /**
     * Clear conversation history.
     */
    fun clearConversation() {
        conversationLog.clear()
        _uiState.value = _uiState.value.copy(conversation = emptyList())
    }

    private fun addToConversation(text: String, isFromUser: Boolean, wasSpoken: Boolean) {
        val message = ConversationMessage(
            text = text,
            isFromUser = isFromUser,
            timestamp = System.currentTimeMillis(),
            wasSpoken = wasSpoken
        )
        conversationLog.add(0, message)  // Most recent first
        if (conversationLog.size > 50) conversationLog.removeLast()

        _uiState.value = _uiState.value.copy(
            conversation = conversationLog.toList()
        )
    }

    override fun onCleared() {
        super.onCleared()
        ttsService.stop()
        speechRecognitionService.release()
    }
}
