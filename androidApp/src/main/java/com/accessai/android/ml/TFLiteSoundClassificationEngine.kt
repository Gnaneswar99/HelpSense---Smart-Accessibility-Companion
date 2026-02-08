package com.accessai.android.ml

import android.content.Context
import com.accessai.core.ml.SoundClassificationEngine
import com.accessai.core.model.InferenceResult
import com.accessai.core.model.SoundEvent
import com.accessai.core.model.SoundPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * TFLite sound classification using YAMNet model.
 *
 * YAMNet classifies audio into 521 sound classes covering speech,
 * music, environmental sounds, and events.
 *
 * Input: 15600 audio samples at 16kHz (~0.975 seconds)
 *        as float32 normalized to [-1, 1]
 * Output: 521-class probability vector
 *
 * We map YAMNet's generic classes to accessibility-relevant categories
 * with priority levels for the alert system.
 */
class TFLiteSoundClassificationEngine(
    private val context: Context,
    private val modelManager: TFLiteModelManager
) : SoundClassificationEngine {

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHUNK_SAMPLES = 15600  // ~0.975 sec at 16kHz
        const val NUM_CLASSES = 521
        const val FLOAT_SIZE = 4
        const val SHORT_MAX = 32768.0f
    }

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    /**
     * Load YAMNet model and labels.
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        interpreter = modelManager.loadModel(
            TFLiteModelManager.MODEL_SOUND_CLASSIFICATION,
            useGpu = false  // Audio models typically run faster on CPU
        )
        labels = modelManager.loadLabels(TFLiteModelManager.LABELS_SOUND)
        interpreter != null
    }

    override suspend fun classify(
        audioData: ShortArray,
        sampleRate: Int
    ): InferenceResult<SoundEvent> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        val interp = interpreter
        if (interp == null) {
            return@withContext InferenceResult.Error(
                "Sound classification model not loaded. Place 'yamnet.tflite' in assets/models/"
            )
        }

        try {
            // Prepare audio input: convert PCM16 to normalized float
            val inputBuffer = audioToFloatBuffer(audioData)

            // Output: scores for each class
            val output = Array(1) { FloatArray(NUM_CLASSES) }

            // Run inference
            interp.run(inputBuffer, output)

            // Find top classification
            val scores = output[0]
            val topIndex = scores.indices.maxByOrNull { scores[it] } ?: 0
            val topScore = scores[topIndex]
            val topLabel = if (topIndex < labels.size) labels[topIndex] else "unknown"

            val inferenceTime = System.currentTimeMillis() - startTime

            // Map to accessibility sound event
            val soundClass = mapToAccessibilityClass(topLabel)
            val priority = getPriorityForClass(soundClass)

            InferenceResult.Success(
                data = SoundEvent(
                    soundClass = soundClass,
                    confidence = topScore,
                    timestamp = System.currentTimeMillis(),
                    priority = priority
                ),
                confidenceScore = topScore,
                inferenceTimeMs = inferenceTime
            )
        } catch (e: Exception) {
            InferenceResult.Error("Sound classification failed: ${e.message}", e)
        }
    }

    /**
     * Convert PCM 16-bit audio to normalized float ByteBuffer.
     * Pads with zeros or truncates to match model input size.
     */
    private fun audioToFloatBuffer(audioData: ShortArray): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(CHUNK_SAMPLES * FLOAT_SIZE)
        buffer.order(ByteOrder.nativeOrder())

        val samplesToProcess = minOf(audioData.size, CHUNK_SAMPLES)

        for (i in 0 until samplesToProcess) {
            buffer.putFloat(audioData[i] / SHORT_MAX)
        }

        // Pad remaining with silence
        for (i in samplesToProcess until CHUNK_SAMPLES) {
            buffer.putFloat(0.0f)
        }

        buffer.rewind()
        return buffer
    }

    /**
     * Map YAMNet class labels to simplified accessibility categories.
     * YAMNet has 521 fine-grained classes; we map them to ~15 relevant categories.
     */
    private fun mapToAccessibilityClass(yamnetLabel: String): String {
        val label = yamnetLabel.lowercase()
        return when {
            // Critical sounds
            label.contains("fire alarm") || label.contains("smoke detector") -> "fire_alarm"
            label.contains("siren") -> "siren"
            label.contains("car horn") || label.contains("honk") -> "car_horn"
            label.contains("glass break") || label.contains("shatter") -> "glass_breaking"
            label.contains("gunshot") || label.contains("explosion") -> "emergency"
            label.contains("scream") || label.contains("shout") -> "scream"

            // High priority sounds
            label.contains("doorbell") || label.contains("ding-dong") -> "doorbell"
            label.contains("knock") -> "door_knock"
            label.contains("telephone") || label.contains("ringtone") -> "phone_ring"
            label.contains("alarm") || label.contains("buzzer") -> "alarm"
            label.contains("baby") && label.contains("cry") -> "baby_cry"
            label.contains("crying") -> "baby_cry"

            // Normal sounds
            label.contains("speech") || label.contains("talk") || label.contains("conversation") -> "speech"
            label.contains("music") || label.contains("singing") -> "music"
            label.contains("dog") && (label.contains("bark") || label.contains("bow")) -> "dog_bark"
            label.contains("cat") && label.contains("meow") -> "cat_meow"
            label.contains("clap") || label.contains("applause") -> "applause"
            label.contains("laughter") -> "laughter"
            label.contains("cough") || label.contains("sneeze") -> "cough"
            label.contains("water") || label.contains("rain") -> "water"
            label.contains("thunder") -> "thunder"
            label.contains("vehicle") || label.contains("engine") || label.contains("car") -> "vehicle"

            // Low priority
            label.contains("silence") || label.contains("quiet") -> "silence"
            label.contains("wind") -> "wind"
            label.contains("noise") || label.contains("static") -> "background_noise"

            else -> "other"
        }
    }

    /**
     * Determine alert priority based on the accessibility sound class.
     */
    private fun getPriorityForClass(soundClass: String): SoundPriority {
        return when (soundClass) {
            "fire_alarm", "siren", "car_horn", "glass_breaking",
            "emergency", "scream" -> SoundPriority.CRITICAL

            "doorbell", "door_knock", "phone_ring", "alarm",
            "baby_cry" -> SoundPriority.HIGH

            "speech", "dog_bark", "cat_meow", "vehicle",
            "thunder", "laughter" -> SoundPriority.NORMAL

            else -> SoundPriority.LOW
        }
    }
}
