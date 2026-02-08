package com.accessai.core.util

import kotlinx.coroutines.flow.Flow

/**
 * Text-to-Speech service interface.
 * Platform implementations wrap Android TTS / iOS AVSpeechSynthesizer.
 */
interface TextToSpeechService {

    /**
     * Speak the given text aloud.
     * @param text The text to speak
     * @param queueMode If true, adds to queue; if false, interrupts current speech
     */
    suspend fun speak(text: String, queueMode: Boolean = false)

    /**
     * Stop any ongoing speech.
     */
    fun stop()

    /**
     * Set speech rate (0.5 = half speed, 1.0 = normal, 2.0 = double).
     */
    fun setSpeechRate(rate: Float)

    /**
     * Set pitch (0.5 = low, 1.0 = normal, 2.0 = high).
     */
    fun setPitch(pitch: Float)

    /**
     * Set the language/locale for speech (e.g., "en-US", "es-ES").
     */
    fun setLanguage(languageCode: String)

    /**
     * Observe TTS state changes (speaking, idle, error).
     */
    fun observeState(): Flow<TTSState>

    /**
     * Release TTS resources.
     */
    fun shutdown()
}

/**
 * TTS engine states.
 */
enum class TTSState {
    IDLE,
    SPEAKING,
    ERROR,
    NOT_INITIALIZED
}

/**
 * Haptic feedback service interface.
 * Provides different vibration patterns for accessibility alerts.
 */
interface HapticService {

    /**
     * Trigger a light tap — used for UI confirmations.
     */
    fun lightTap()

    /**
     * Trigger a medium impact — used for detected objects/sounds.
     */
    fun mediumImpact()

    /**
     * Trigger a heavy impact — used for critical alerts (fire alarm, siren).
     */
    fun heavyImpact()

    /**
     * Trigger a custom vibration pattern.
     * @param pattern Array of durations in ms [wait, vibrate, wait, vibrate, ...]
     */
    fun customPattern(pattern: LongArray)

    /**
     * Trigger a success notification pattern.
     */
    fun success()

    /**
     * Trigger a warning notification pattern.
     */
    fun warning()

    /**
     * Trigger an error/critical notification pattern.
     */
    fun error()
}

/**
 * Camera frame data passed from platform camera to shared ML pipeline.
 */
data class CameraFrame(
    val data: ByteArray,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val timestamp: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is CameraFrame) return false
        return data.contentEquals(other.data) &&
                width == other.width &&
                height == other.height &&
                rotationDegrees == other.rotationDegrees &&
                timestamp == other.timestamp
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + rotationDegrees
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * Camera service interface for accessing device camera.
 * Platform implementations wrap CameraX (Android) / AVFoundation (iOS).
 */
interface CameraService {
    /**
     * Stream of camera frames for ML processing.
     */
    fun frameStream(): Flow<CameraFrame>

    /**
     * Capture a single high-resolution image.
     * @return JPEG bytes
     */
    suspend fun captureImage(): ByteArray

    /**
     * Check if camera is currently active.
     */
    fun isActive(): Boolean

    /**
     * Toggle between front and back camera.
     */
    suspend fun switchCamera()

    /**
     * Release camera resources.
     */
    fun release()
}

/**
 * Audio capture service interface.
 * Platform implementations wrap AudioRecord (Android) / AVAudioEngine (iOS).
 */
interface AudioCaptureService {
    /**
     * Stream of audio chunks for ML sound classification.
     * Each chunk is a short PCM audio segment.
     */
    fun audioStream(): Flow<AudioChunk>

    /**
     * Start recording audio.
     */
    suspend fun startCapture()

    /**
     * Stop recording audio.
     */
    suspend fun stopCapture()

    /**
     * Check if currently recording.
     */
    fun isCapturing(): Boolean
}

/**
 * Raw audio data chunk for ML processing.
 */
data class AudioChunk(
    val data: ShortArray,
    val sampleRate: Int,
    val channelCount: Int,
    val timestamp: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is AudioChunk) return false
        return data.contentEquals(other.data) &&
                sampleRate == other.sampleRate &&
                channelCount == other.channelCount &&
                timestamp == other.timestamp
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + channelCount
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
