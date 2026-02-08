package com.accessai.android.ml

import com.accessai.core.model.AudioRepository
import com.accessai.core.model.InferenceResult
import com.accessai.core.model.SoundEvent
import com.accessai.core.util.AudioCaptureService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Android implementation of AudioRepository.
 * Connects the audio capture pipeline to the sound classification ML engine.
 *
 * Flow: Microphone → AudioCaptureService → AudioChunks → SoundClassificationEngine → SoundEvents
 */
class AndroidAudioRepository(
    private val audioCaptureService: AudioCaptureService,
    private val soundClassificationEngine: TFLiteSoundClassificationEngine
) : AudioRepository {

    private var _isListening = false

    /**
     * Start listening: captures audio chunks and classifies each one.
     * Emits SoundEvent results that can be filtered by the Use Case layer.
     */
    override fun startListening(): Flow<InferenceResult<SoundEvent>> = flow {
        _isListening = true

        audioCaptureService.audioStream().collect { chunk ->
            if (!_isListening) return@collect

            // Run ML classification on each audio chunk
            val result = soundClassificationEngine.classify(
                audioData = chunk.data,
                sampleRate = chunk.sampleRate
            )

            // Only emit if confidence is above threshold
            when (result) {
                is InferenceResult.Success -> {
                    if (result.confidenceScore > 0.3f) {
                        emit(result)
                    }
                }
                is InferenceResult.Error -> emit(result)
                is InferenceResult.Loading -> emit(result)
            }
        }
    }

    override suspend fun stopListening() {
        _isListening = false
        audioCaptureService.stopCapture()
    }

    override fun isListening(): Boolean = _isListening
}
