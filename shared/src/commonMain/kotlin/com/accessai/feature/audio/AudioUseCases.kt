package com.accessai.feature.audio

import com.accessai.core.model.AudioRepository
import com.accessai.core.model.InferenceResult
import com.accessai.core.model.SoundEvent
import com.accessai.core.model.SoundPriority
import com.accessai.core.model.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * Use case for monitoring ambient sounds.
 * Filters sound events based on user preferences and priority.
 */
class MonitorSoundsUseCase(
    private val audioRepository: AudioRepository,
    private val preferencesRepository: PreferencesRepository
) {
    /**
     * Start monitoring and return filtered sound events
     * based on user's alert preferences.
     */
    operator fun invoke(): Flow<InferenceResult<SoundEvent>> {
        return audioRepository.startListening().filter { result ->
            when (result) {
                is InferenceResult.Success -> {
                    val prefs = preferencesRepository.getPreferences()
                    val event = result.data
                    // Always pass through critical sounds
                    if (event.priority == SoundPriority.CRITICAL) return@filter true
                    // Check user preferences for this sound class
                    prefs.soundAlertProfiles[event.soundClass] ?: false
                }
                is InferenceResult.Error -> true  // Always propagate errors
                is InferenceResult.Loading -> true
            }
        }
    }

    /**
     * Stop the sound monitor.
     */
    suspend fun stop() {
        audioRepository.stopListening()
    }
}

/**
 * Use case for classifying a specific audio clip.
 */
class ClassifySoundUseCase(
    private val audioRepository: AudioRepository
) {
    fun isMonitoring(): Boolean = audioRepository.isListening()
}
