package com.accessai.android.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.accessai.core.util.TTSState
import com.accessai.core.util.TextToSpeechService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Android implementation of TextToSpeechService.
 * Wraps the Android TTS engine with coroutine support.
 *
 * Features:
 * - Queue or interrupt speech modes
 * - Configurable rate, pitch, and language
 * - State observation via Flow
 */
class AndroidTextToSpeechService(
    private val context: Context
) : TextToSpeechService {

    private var tts: TextToSpeech? = null
    private val _state = MutableStateFlow(TTSState.NOT_INITIALIZED)
    private var speechRate = 1.0f
    private var pitch = 1.0f
    private var language = Locale.US

    init {
        initializeTTS()
    }

    private fun initializeTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    engine.language = language
                    engine.setSpeechRate(speechRate)
                    engine.setPitch(pitch)

                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _state.value = TTSState.SPEAKING
                        }

                        override fun onDone(utteranceId: String?) {
                            _state.value = TTSState.IDLE
                        }

                        @Deprecated("Deprecated in API")
                        override fun onError(utteranceId: String?) {
                            _state.value = TTSState.ERROR
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            _state.value = TTSState.ERROR
                        }
                    })
                }
                _state.value = TTSState.IDLE
            } else {
                _state.value = TTSState.ERROR
            }
        }
    }

    override suspend fun speak(text: String, queueMode: Boolean) {
        val engine = tts ?: return

        if (_state.value == TTSState.NOT_INITIALIZED) return

        val mode = if (queueMode) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH
        val utteranceId = UUID.randomUUID().toString()

        // Wait for speech to complete
        suspendCoroutine<Unit> { continuation ->
            val originalListener = engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _state.value = TTSState.SPEAKING
                }

                override fun onDone(id: String?) {
                    _state.value = TTSState.IDLE
                    if (id == utteranceId) {
                        continuation.resume(Unit)
                    }
                }

                @Deprecated("Deprecated in API")
                override fun onError(utteranceId: String?) {
                    _state.value = TTSState.ERROR
                    continuation.resume(Unit)
                }

                override fun onError(id: String?, errorCode: Int) {
                    _state.value = TTSState.ERROR
                    if (id == utteranceId) {
                        continuation.resume(Unit)
                    }
                }
            })

            engine.speak(text, mode, null, utteranceId)
        }
    }

    override fun stop() {
        tts?.stop()
        _state.value = TTSState.IDLE
    }

    override fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.1f, 4.0f)
        tts?.setSpeechRate(speechRate)
    }

    override fun setPitch(pitch: Float) {
        this.pitch = pitch.coerceIn(0.1f, 4.0f)
        tts?.setPitch(this.pitch)
    }

    override fun setLanguage(languageCode: String) {
        language = Locale.forLanguageTag(languageCode)
        tts?.language = language
    }

    override fun observeState(): Flow<TTSState> = _state.asStateFlow()

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _state.value = TTSState.NOT_INITIALIZED
    }
}
